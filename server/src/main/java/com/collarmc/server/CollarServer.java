package com.collarmc.server;

import com.collarmc.protocol.SessionStopReason;
import com.collarmc.protocol.devices.RegisterDeviceResponse;
import com.collarmc.protocol.identity.IdentifyResponse;
import com.collarmc.api.identity.ServerIdentity;
import com.collarmc.server.protocol.*;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.collarmc.api.http.RequestContext;
import com.collarmc.api.profiles.Profile;
import com.collarmc.api.profiles.ProfileService.UpdateProfileRequest;
import com.collarmc.api.session.Player;
import com.collarmc.protocol.PacketIO;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.protocol.identity.IdentifyRequest;
import com.collarmc.protocol.keepalive.KeepAliveRequest;
import com.collarmc.protocol.keepalive.KeepAliveResponse;
import com.collarmc.protocol.session.SessionFailedResponse.MojangVerificationFailedResponse;
import com.collarmc.protocol.session.SessionFailedResponse.PrivateIdentityMismatchResponse;
import com.collarmc.protocol.session.StartSessionRequest;
import com.collarmc.protocol.session.StartSessionResponse;
import com.collarmc.protocol.trust.CheckTrustRelationshipRequest;
import com.collarmc.protocol.trust.CheckTrustRelationshipResponse;
import com.collarmc.protocol.trust.CheckTrustRelationshipResponse.IsTrustedRelationshipResponse;
import com.collarmc.protocol.trust.CheckTrustRelationshipResponse.IsUntrustedRelationshipResponse;
import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.security.TokenGenerator;
import com.collarmc.security.messages.CipherException;
import com.collarmc.security.mojang.MinecraftPlayer;
import com.collarmc.security.mojang.Mojang;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
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

        protocolHandlers.add(new GroupsProtocolHandler(services.groups));
        protocolHandlers.add(new LocationProtocolHandler(services.playerLocations, services.waypoints, services.identityStore.identity()));
        protocolHandlers.add(new TexturesProtocolHandler(services.identityStore.identity(), services.profileCache, services.sessions, services.textures));
        protocolHandlers.add(new IdentityProtocolHandler(services.sessions, services.profiles, services.identityStore.identity()));
        protocolHandlers.add(new MessagingProtocolHandler(services.sessions, services.groups, services.identityStore.identity()));
        protocolHandlers.add(new SDHTProtocolHandler(services.groups, services.sessions, services.identityStore.identity()));
        protocolHandlers.add(new FriendsProtocolHandler(services.identityStore.identity(), services.profileCache, services.friends, services.sessions));
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
        Optional<ProtocolRequest> requestOptional = read(session, is);
        requestOptional.ifPresent(req -> {
            LOGGER.debug(req.getClass().getSimpleName() + " from " + req.identity);
            ServerIdentity serverIdentity = services.identityStore.identity();
            if (req instanceof KeepAliveRequest) {
                LOGGER.debug("KeepAliveRequest received. Sending KeepAliveRequest.");
                sendPlain(session, new KeepAliveResponse(serverIdentity));
            } else if (req instanceof IdentifyRequest) {
                IdentifyRequest request = (IdentifyRequest)req;
                if (request.identity == null) {
                    LOGGER.debug("Signaling client to register");
                    String token = services.deviceRegistration.createDeviceRegistrationToken(session);
                    String url = services.urlProvider.deviceVerificationUrl(token);
                    sendPlain(session, new RegisterDeviceResponse(serverIdentity, url, token));
                } else {
                    services.profileCache.getById(req.identity.id()).ifPresentOrElse(profile -> {
                        if (processPrivateIdentityToken(profile, request)) {
                            LOGGER.debug("Profile found for " + req.identity.id());
                            // TODO: is this the right place?
                            if (!services.identityStore.isTrustedIdentity(req.identity)) {
                                services.identityStore.trustIdentity(req.identity);
                            }
                            sendPlain(session, new IdentifyResponse(serverIdentity, profile.toPublic(), Mojang.serverPublicKey(), TokenGenerator.byteToken(16)));
                        } else {
                            sendPlain(session, new PrivateIdentityMismatchResponse(serverIdentity, services.urlProvider.resetPrivateIdentity()));
                        }
                    }, () -> {
                        LOGGER.error("Profile " + request.identity.id() + " does not exist but the client thinks it should.");
                        sendPlain(session, new IsUntrustedRelationshipResponse(serverIdentity));
                        services.sessions.stopSession(session, SessionStopReason.UNAUTHORISED, "Identity " + request.identity.id() + " was not found", null, null);
                    });
                }
            } else if (req instanceof StartSessionRequest) {
                LOGGER.info("Starting session with " + req.identity);
                StartSessionRequest request = (StartSessionRequest)req;
                if (services.minecraftSessionVerifier.verify(request)) {
                    MinecraftPlayer minecraftPlayer = request.session.toPlayer();
                    services.sessions.identify(session, req.identity, minecraftPlayer);
                    services.profiles.updateProfile(RequestContext.SERVER, UpdateProfileRequest.addMinecraftAccount(req.identity.id(), request.session.id));
                    sendPlain(session, new StartSessionResponse(serverIdentity));
                } else {
                    sendPlain(session, new MojangVerificationFailedResponse(serverIdentity, ((StartSessionRequest) req).session));
                    services.sessions.stopSession(session, SessionStopReason.UNAUTHORISED, "Minecraft session invalid", null, sessionStopped);
                }
            } else if (req instanceof CheckTrustRelationshipRequest) {
                LOGGER.info("Checking if client/server have a trusted relationship");
                if (services.identityStore.isTrustedIdentity(req.identity)) {
                    LOGGER.info(req.identity + " is trusted. Signaling client to start encryption. ");
                    CheckTrustRelationshipResponse response = new IsTrustedRelationshipResponse(serverIdentity);
                    sendPlain(session, response);
                    services.sessions.findPlayer(req.identity).ifPresent(player -> {
                        sessionStarted.accept(req.identity, new Player(player.identity, player.minecraftPlayer));
                    });
                } else {
                    LOGGER.info(req.identity + " is NOT trusted. Signaling client to restart registration.");
                    CheckTrustRelationshipResponse response = new IsUntrustedRelationshipResponse(serverIdentity);
                    sendPlain(session, response);
                    services.sessions.stopSession(session, SessionStopReason.UNAUTHORISED, req.identity + " identity is not trusted", null, null);
                }
            } else {
                for (ProtocolHandler handler : protocolHandlers) {
                    if (handler.handleRequest(this, req, createSender())) {
                        break;
                    }
                }
            }
        });
    }

    private boolean processPrivateIdentityToken(Profile profile, IdentifyRequest req) {
        if (profile.privateIdentityToken == null || profile.privateIdentityToken.length == 0) {
            services.profiles.updateProfile(RequestContext.SERVER, UpdateProfileRequest.privateIdentityToken(profile.id, req.token));
            return true;
        }
        return Arrays.equals(profile.privateIdentityToken, req.token);
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
            Optional<ProtocolRequest> packet = packetIO.decode(identity, message, ProtocolRequest.class);
            if (packet.isPresent() && packet.get().identity != null && identity != null && !packet.get().identity.equals(identity)) {
                throw new IllegalStateException("Identity associated with this session was different to decoded packet");
            }
            return packet;
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
