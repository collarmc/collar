package com.collarmc.server;

import com.collarmc.api.http.HttpException.NotFoundException;
import com.collarmc.api.http.RequestContext;
import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.identity.ServerIdentity;
import com.collarmc.api.profiles.Profile;
import com.collarmc.api.profiles.ProfileService;
import com.collarmc.api.profiles.ProfileService.UpdateProfileRequest;
import com.collarmc.api.session.Player;
import com.collarmc.protocol.PacketIO;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.protocol.SessionStopReason;
import com.collarmc.protocol.devices.RegisterClientResponse;
import com.collarmc.protocol.identity.IdentifyRequest;
import com.collarmc.protocol.identity.IdentifyResponse;
import com.collarmc.protocol.keepalive.KeepAliveRequest;
import com.collarmc.protocol.keepalive.KeepAliveResponse;
import com.collarmc.protocol.session.SessionFailedResponse.MojangVerificationFailedResponse;
import com.collarmc.protocol.session.SessionFailedResponse.PrivateIdentityMismatchResponse;
import com.collarmc.protocol.session.StartSessionRequest;
import com.collarmc.protocol.session.StartSessionResponse;
import com.collarmc.security.messages.CipherException;
import com.collarmc.security.mojang.MinecraftPlayer;
import com.collarmc.security.mojang.Mojang;
import com.collarmc.server.protocol.*;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;

@WebSocket
public class CollarServer {
    private static final Logger LOGGER = LogManager.getLogger(CollarServer.class.getName());

    private final List<ProtocolHandler> protocolHandlers;
    private final BiConsumer<ClientIdentity, Player> sessionStarted;
    private final BiConsumer<ClientIdentity, Player> sessionStopped;
    private final ConcurrentMap<Session, Bucket> buckets = new ConcurrentHashMap<>();
    private final Services services;

    public CollarServer(Services services) {
        this.services = services;
        this.protocolHandlers = new ArrayList<>();
        this.sessionStarted = (identity, player) -> protocolHandlers.forEach(protocolHandler -> protocolHandler.onSessionStarted(identity, player, this::send));
        this.sessionStopped = (identity, player) -> protocolHandlers.forEach(protocolHandler -> protocolHandler.onSessionStopping(identity, player, this::send));

        protocolHandlers.add(new GroupsProtocolHandler(services));
        protocolHandlers.add(new LocationProtocolHandler(services));
        protocolHandlers.add(new TexturesProtocolHandler(services));
        protocolHandlers.add(new IdentityProtocolHandler(services));
        protocolHandlers.add(new MessagingProtocolHandler(services));
        protocolHandlers.add(new SDHTProtocolHandler(services));
        protocolHandlers.add(new FriendsProtocolHandler(services));
    }

    @OnWebSocketConnect
    public void connected(Session session) {
        LOGGER.info("New socket connected");
        buckets.computeIfAbsent(session, theSession -> Bucket4j.builder()
                .addLimit(Bandwidth.simple(18000, Duration.ofSeconds(3600)))
                .addLimit(Bandwidth.simple(50, Duration.ofSeconds(1)))
                .build());
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        LOGGER.info("Session closed " + statusCode + " " + reason);
        services.sessions.stopSession(session, SessionStopReason.NORMAL_CLOSE, null, null, sessionStopped);
        services.deviceRegistration.onSessionClosed(session);
        buckets.remove(session);
    }

    @OnWebSocketError
    public void onError(Session session, Throwable e) {
        LOGGER.error("Unrecoverable error " + e.getMessage(), e);
        services.sessions.stopSession(session, SessionStopReason.SERVER_ERROR, null, e, sessionStopped);
    }

    @OnWebSocketMessage
    public void message(Session session, InputStream is) throws IOException {
        Bucket bucket = buckets.get(session);
        if (bucket.tryConsume(1)) {
            processMessage(session, is);
        } else {
            services.sessions.stopSession(session, SessionStopReason.TOO_MANY_REQUESTS, null, null, sessionStopped);
        }
    }

    private void processMessage(Session session, InputStream is) {
        read(session, is).ifPresent(req -> {
            ServerIdentity serverIdentity = services.identityStore.identity();
            if (req instanceof KeepAliveRequest) {
                sendPlain(session, new KeepAliveResponse());
            } else if (req instanceof IdentifyRequest) {
                IdentifyRequest request = (IdentifyRequest)req;
                if (request.identity == null) {
                    LOGGER.debug("Signaling client to register");
                    String token = services.deviceRegistration.createClientRegistrationToken(session);
                    String url = services.urlProvider.deviceVerificationUrl(token);
                    sendPlain(session, new RegisterClientResponse(url, token));
                } else {
                    try {
                        Profile profile = services.profiles.getProfile(RequestContext.SERVER, ProfileService.GetProfileRequest.byId(request.identity.id())).profile;
                        LOGGER.debug("Profile found for " + request.identity.id());
                        byte[] token = processIdentityRequestToken(profile, request);
                        if (token != null) {
                            services.sessions.identify(session, request.identity, null, sessionStarted);
                            byte[] cipherToken = services.identityStore.cipher().encrypt(token, request.identity);
                            sendPlain(session, new IdentifyResponse(serverIdentity, profile.toPublic(), Mojang.serverPublicKey(), Mojang.generateSharedSecret(), cipherToken));
                        } else {
                            sendPlain(session, new PrivateIdentityMismatchResponse(services.urlProvider.resetPrivateIdentity()));
                        }
                    } catch (CipherException e) {
                        LOGGER.error("Problem preparing token for identity response", e);
                        services.sessions.stopSession(session, SessionStopReason.SERVER_ERROR, "Problem preparing Identity response", null, null);
                    } catch (NotFoundException e) {
                        LOGGER.error("Profile " + request.identity.id() + " does not exist but the client thinks it should.");
                        services.sessions.stopSession(session, SessionStopReason.UNAUTHORISED, "Identity " + request.identity.id() + " was not found", null, null);
                    }
                }
            } else if (req instanceof StartSessionRequest) {
                ClientIdentity identity = services.sessions.getIdentity(session).orElseThrow(() -> new IllegalStateException("session must have an identity"));
                LOGGER.info("Starting session with " + identity);
                StartSessionRequest request = (StartSessionRequest)req;
                if (services.minecraftSessionVerifier.verify(request)) {
                    MinecraftPlayer minecraftPlayer = request.session.toPlayer();
                    services.sessions.identify(session, identity, minecraftPlayer, sessionStarted);
                    services.profiles.updateProfile(RequestContext.SERVER, UpdateProfileRequest.addMinecraftAccount(identity.id(), request.session.id));
                    sendPlain(session, new StartSessionResponse());
                } else {
                    sendPlain(session, new MojangVerificationFailedResponse(request.session));
                    services.sessions.stopSession(session, SessionStopReason.UNAUTHORISED, "Minecraft session invalid", null, sessionStopped);
                }
            } else {
                ClientIdentity identity = services.sessions.getIdentity(session).orElseThrow(() -> new IllegalStateException("session must have an identity"));
                for (ProtocolHandler handler : protocolHandlers) {
                    if (handler.handleRequest(this, identity, req, createSender())) {
                        break;
                    }
                }
            }
        });
    }

    private byte[] processIdentityRequestToken(Profile profile, IdentifyRequest req) {
        if (profile.publicKey == null) {
            profile = services.profiles.updateProfile(RequestContext.SERVER, UpdateProfileRequest.keys(profile.id, req.identity.publicKey())).profile;
            LOGGER.log(Level.ERROR, "Identity for " + profile.id + " was set");
        }
        ClientIdentity storedIdentity = new ClientIdentity(profile.id, profile.publicKey);
        if (!storedIdentity.equals(req.identity)) {
            LOGGER.log(Level.ERROR, "Identity for " + profile.id + " did not match what was registered");
            return null;
        }
        try {
            byte[] token = services.identityStore.cipher().decrypt(req.token, req.identity.publicKey());
            LOGGER.log(Level.INFO, "Token from " + profile.id + " was decrypted");
            return token;
        } catch (CipherException e) {
            LOGGER.log(Level.ERROR, "Could not decrypt token", e);
            return null;
        }
    }

    private BiConsumer<ClientIdentity, ProtocolResponse> createSender() {
        return (identity, response) -> {
            if (identity == null) {
                send(null, response);
            } else {
                services.sessions.getSession(identity).ifPresent(recipientSession -> {
                    send(recipientSession, response);
                });
            }
        };
    }

    @Nonnull
    public Optional<ProtocolRequest> read(@Nonnull Session session, @Nonnull InputStream message) {
        PacketIO packetIO = new PacketIO(services.packetMapper, services.identityStore.cipher());
        ClientIdentity identity = services.sessions.getIdentity(session).orElse(null);
        try {
            return packetIO.decode(identity, message, ProtocolRequest.class);
        } catch (IOException | CipherException e) {
            throw new IllegalStateException(e);
        }
    }

    public void send(Session session, ProtocolResponse resp) {
        if (session != null && !session.isOpen()) {
            return;
        }
        if (resp instanceof BatchProtocolResponse) {
            BatchProtocolResponse batchResponse = (BatchProtocolResponse)resp;
            batchResponse.responses.forEach((response, identity) -> {
                services.sessions.getSession(identity).ifPresent(anotherSession -> {
                    send(anotherSession, response);
                });
            });
        } else {
            LOGGER.info("Sending " + resp.getClass().getSimpleName());
            if (session == null) {
                throw new IllegalStateException("Session cannot be null");
            }
            PacketIO packetIO = new PacketIO(services.packetMapper, services.identityStore.cipher());
            byte[] bytes;
            if (services.sessions.isIdentified(session)) {
                try {
                    ClientIdentity identity = services.sessions.getIdentity(session).orElseThrow(() -> new IllegalStateException("Could not find identity"));
                    bytes = packetIO.encodeEncrypted(identity, resp);
                } catch (IOException | CipherException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                try {
                    bytes = packetIO.encodePlain(resp);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            sendBytes(session, bytes);
        }
    }

    public void sendPlain(@Nonnull Session session, @Nonnull ProtocolResponse resp) {
        if (!session.isOpen()) {
            return;
        }
        PacketIO packetIO = new PacketIO(services.packetMapper, null);
        byte[] bytes;
        try {
            bytes = packetIO.encodePlain(resp);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        sendBytes(session, bytes);
    }

    private void sendBytes(@Nonnull Session session, @Nonnull byte[] bytes) {
        session.getRemote().sendBytesByFuture(ByteBuffer.wrap(bytes));
    }
}
