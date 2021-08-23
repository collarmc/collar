package com.collarmc.client;

import com.collarmc.api.http.CollarFeature;
import com.collarmc.api.http.CollarVersion;
import com.collarmc.api.http.DiscoverResponse;
import com.collarmc.api.http.HttpException;
import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.identity.ServerIdentity;
import com.collarmc.api.session.Player;
import com.collarmc.client.CollarException.ConnectionException;
import com.collarmc.client.api.AbstractApi;
import com.collarmc.client.api.ApiListener;
import com.collarmc.client.api.friends.FriendsApi;
import com.collarmc.client.api.groups.GroupsApi;
import com.collarmc.client.api.identity.IdentityApi;
import com.collarmc.client.api.location.LocationApi;
import com.collarmc.client.api.messaging.MessagingApi;
import com.collarmc.client.api.textures.TexturesApi;
import com.collarmc.client.minecraft.Ticks;
import com.collarmc.client.sdht.SDHTApi;
import com.collarmc.client.sdht.cipher.ContentCiphers;
import com.collarmc.client.sdht.cipher.GroupContentCipher;
import com.collarmc.client.security.ClientIdentityStore;
import com.collarmc.client.security.ClientIdentityStoreImpl;
import com.collarmc.client.utils.Crypto;
import com.collarmc.client.utils.Http;
import com.collarmc.http.Request;
import com.collarmc.http.Response;
import com.collarmc.http.WebSocket;
import com.collarmc.http.WebSocketListener;
import com.collarmc.protocol.PacketIO;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.protocol.SessionStopReason;
import com.collarmc.protocol.devices.DeviceRegisteredResponse;
import com.collarmc.protocol.devices.RegisterDeviceResponse;
import com.collarmc.protocol.identity.IdentifyResponse;
import com.collarmc.protocol.keepalive.KeepAliveResponse;
import com.collarmc.protocol.session.SessionFailedResponse;
import com.collarmc.protocol.session.SessionFailedResponse.MojangVerificationFailedResponse;
import com.collarmc.protocol.session.SessionFailedResponse.PrivateIdentityMismatchResponse;
import com.collarmc.protocol.session.SessionFailedResponse.SessionErrorResponse;
import com.collarmc.protocol.session.StartSessionRequest;
import com.collarmc.protocol.session.StartSessionResponse;
import com.collarmc.security.messages.CipherException;
import com.collarmc.security.messages.CipherException.InvalidCipherSessionException;
import com.collarmc.security.mojang.MinecraftSession;
import com.collarmc.security.mojang.Mojang;
import com.collarmc.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mikael.urlbuilder.UrlBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.collarmc.http.Request.url;

public final class Collar {
    private static final Logger LOGGER = LogManager.getLogger(Collar.class.getName());
    private static final CollarVersion VERSION = new CollarVersion(0, 1);

    public final CollarConfiguration configuration;
    private final GroupsApi groupsApi;
    private final LocationApi locationApi;
    private final TexturesApi texturesApi;
    private final FriendsApi friendsApi;
    private final IdentityApi identityApi;
    private final MessagingApi messagingApi;
    private final SDHTApi sdhtApi;
    private WebSocket webSocket;
    private volatile State state;
    private final List<AbstractApi<? extends ApiListener>> apis;
    private Consumer<ProtocolRequest> sender;
    private ClientIdentityStore identityStore;
    private final Supplier<ClientIdentityStore> identityStoreSupplier;
    private final Ticks ticks;
    private final ContentCiphers recordCiphers;

    private Collar(CollarConfiguration configuration) throws IOException {
        this.configuration = configuration;
        changeState(State.DISCONNECTED);
        this.identityStoreSupplier = () -> this.identityStore;
        Consumer<ProtocolRequest> sender = request -> this.sender.accept(request);
        this.ticks = configuration.ticks;
        this.recordCiphers = new ContentCiphers();
        this.apis = new ArrayList<>();
        this.sdhtApi = new SDHTApi(this, identityStoreSupplier, sender, recordCiphers, this.ticks, this.configuration.homeDirectory.dhtState());
        this.groupsApi = new GroupsApi(this, identityStoreSupplier, sender, sdhtApi);
        this.recordCiphers.register(new GroupContentCipher(groupsApi, identityStoreSupplier));
        this.locationApi = new LocationApi(this,
                identityStoreSupplier,
                sender,
                ticks,
                groupsApi,
                sdhtApi,
                configuration.playerLocation,
                configuration.entitiesSupplier);
        this.texturesApi = new TexturesApi(this, identityStoreSupplier, sender);
        this.identityApi = new IdentityApi(this, identityStoreSupplier, sender);
        this.messagingApi = new MessagingApi(this, identityStoreSupplier, sender);
        this.friendsApi = new FriendsApi(this, identityStoreSupplier, sender);
        this.apis.add(groupsApi);
        this.apis.add(locationApi);
        this.apis.add(texturesApi);
        this.apis.add(friendsApi);
        this.apis.add(identityApi);
        this.apis.add(messagingApi);
        this.apis.add(sdhtApi);
        addStateSavingShutdownHook(this);
    }

    /**
     * Create a new Collar client
     * @param configuration of the client
     */
    public static Collar create(CollarConfiguration configuration) {
        try {
            return new Collar(configuration);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Connect to server
     */
    public void connect() {
        if (!configuration.homeDirectory.getLock().tryLock()) {
            configuration.listener.onError(this, "Collar cannot connect as there is another client using Collar. Disconnect the other client.");
            return;
        }
        try {
            checkServerCompatibility(configuration);
            String url = UrlBuilder.fromUrl(configuration.collarServerURL).withPath("/api/1/listen").toString();
            LOGGER.info("Connecting to server " + url);
            webSocket = Http.client().webSocket(Request.url(url).ws(), new CollarWebSocket(this));
            changeState(State.CONNECTING);
        } catch (CollarException e) {
            changeState(State.DISCONNECTED);
            throw e;
        } catch (Throwable e) {
            changeState(State.DISCONNECTED);
            throw new ConnectionException("Failed to connect", e);
        }
    }

    /**
     * Disconnect from server
     */
    public void disconnect() {
        if (this.webSocket != null) {
            LOGGER.info("Disconnected");
            this.webSocket.close();
            this.webSocket = null;
            if (this.identityStore != null) {
                this.identityStore.clearAllGroupSessions();
            }
            if (state == State.DISCONNECTED) {
                return;
            }
            changeState(State.DISCONNECTED);
            if (identityStore != null) {
                try {
                    identityStore.save();
                } catch (IOException e) {
                    LOGGER.error("Could not save Collar signal state", e);
                }
            }
        }
    }

    /**
     * @return groups api
     */
    public GroupsApi groups() {
        assertConnected();
        return groupsApi;
    }

    /**
     * @return location api
     */
    public LocationApi location() {
        assertConnected();
        return locationApi;
    }

    /**
     * @return texture api
     */
    public TexturesApi textures() {
        assertConnected();
        return texturesApi;
    }

    /**
     * @return identity api
     */
    public IdentityApi identities() {
        assertConnected();
        return identityApi;
    }

    /**
     * @return friends api
     */
    public FriendsApi friends() {
        assertConnected();
        return friendsApi;
    }

    /**
     * @return messaging api
     */
    public MessagingApi messaging() {
        assertConnected();
        return messagingApi;
    }

    /**
     * @return client state
     */
    public State getState() {
        return state;
    }

    /**
     * @return the current clients identity
     */
    public ClientIdentity identity() {
        return identityStore != null ? identityStore.identity() : null;
    }

    /**
     * Change the client state and fire the listener
     * @param state to change to
     */
    private void changeState(State state) {
        State previousState = this.state;
        if (previousState != state) {
            this.state = state;
            if (previousState != null && previousState != State.DISCONNECTED && state == State.DISCONNECTED) {
                disconnect();
            }
            if (previousState == null) {
                LOGGER.info("client in state " + state);
            } else {
                if (identityStore != null) {
                    LOGGER.info(identityStore.identity() + " state changed from " + previousState + " to " + state);
                } else {
                    LOGGER.info("state changed from " + previousState + " to " + state);
                }
            }
            if (previousState != null) {
                this.configuration.listener.onStateChanged(this, state);
                apis.forEach(abstractApi -> abstractApi.onStateChanged(state));
            }
        }
        if (state == State.DISCONNECTED) {
            this.configuration.homeDirectory.getLock().unlock();
        }
    }

    /**
     * The current player
     * @return player
     */
    public Player player() {
        assertConnected();
        return new Player(identity(), configuration.sessionSupplier.get().toPlayer());
    }

    private void assertConnected() {
        if (state != State.CONNECTED) {
            throw new IllegalStateException("Cannot use the API until client is CONNECTED");
        }
    }

    /**
     * Test that the client version is supported by the server and that the client is configured correctly for its features
     * @param configuration of the client
     */
    private static void checkServerCompatibility(CollarConfiguration configuration) {
        DiscoverResponse response;
        try {
            response = Http.client().execute(url(UrlBuilder.fromUrl(configuration.collarServerURL).withPath("/api/discover")).get(), Response.json(DiscoverResponse.class));
        } catch (HttpException e) {
            throw new ConnectionException("Problem connecting to collar", e);
        }
        StringJoiner versions = new StringJoiner(",");
        response.versions.stream()
                .peek(collarVersion -> versions.add(collarVersion.major + "." + collarVersion.minor))
                .filter(collarVersion -> collarVersion.equals(VERSION))
                .findFirst()
                .orElseThrow(() -> new CollarException.UnsupportedServerVersionException(VERSION + " is not supported by server. Server supports versions " + versions.toString()));
        Optional<CollarFeature> verificationScheme = findFeature(response, "auth:verification_scheme");
        if (!verificationScheme.isPresent()) {
            throw new IllegalStateException("Does not have feature auth:verification_scheme");
        }
        verificationScheme.ifPresent(collarFeature -> {
            MinecraftSession minecraftSession = configuration.sessionSupplier.get();
            LOGGER.info("Server supports versions " + versions);
            if ("mojang".equals(collarFeature.value) && minecraftSession.mode != MinecraftSession.Mode.MOJANG && minecraftSession.accessToken == null) {
                throw new IllegalStateException("mojang verification scheme requested but was provided an invalid MinecraftSession");
            } else if ("nojang".equals(collarFeature.value) && minecraftSession.mode != MinecraftSession.Mode.NOJANG && minecraftSession.accessToken != null) {
                throw new IllegalStateException("nojang verification scheme requested but was provided an invalid MinecraftSession");
            }
        });
        findFeature(response, "groups:locations").orElseThrow(() -> new IllegalStateException("Server does not support groups:locations"));
        findFeature(response, "groups:waypoints").orElseThrow(() -> new IllegalStateException("Server does not support groups:waypoints"));
        findFeature(response, "profile:friends").orElseThrow(() -> new IllegalStateException("Server does not support profile:friends"));
    }

    private static Optional<CollarFeature> findFeature(DiscoverResponse response, String feature) {
        return response.features.stream()
                .filter(collarFeature -> feature.equals(collarFeature.name))
                .findFirst();
    }

    /**
     * Saves signal store state on shutdown
     * @param collar instance
     */
    private static void addStateSavingShutdownHook(Collar collar) {
        WeakReference<Collar> collarWeakRef = new WeakReference<>(collar);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Collar instance = collarWeakRef.get();
            if (instance != null && instance.identityStore != null) {
                try {
                    instance.identityStore.save();
                } catch (IOException e) {
                    LOGGER.error("Could not save Collar signal state to disk", e);
                }
                // Unlock
                instance.configuration.homeDirectory.getLock().unlock();
            }
        }));
    }

    class CollarWebSocket implements WebSocketListener {
        private final ObjectMapper mapper = Utils.messagePackMapper();
        private final Collar collar;
        private KeepAlive keepAlive;
        private volatile ServerIdentity serverIdentity;

        public CollarWebSocket(Collar collar) {
            this.collar = collar;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            // Create the sender delegate
            sender = request -> {
                if (state != State.CONNECTED) {
                    throw new IllegalStateException("Client is not in CONNECTED state");
                }
                sendRequest(webSocket, request);
            };
            LOGGER.info("Connection established");
            try {
                identityStore = new ClientIdentityStoreImpl(configuration.homeDirectory);
            } catch (IOException | CipherException e) {
                throw new IllegalStateException("could not load identity store");
            }
            // Start protocol
            sendRequest(webSocket, identityStore.createIdentifyRequest());
            // Start the keep alive
            this.keepAlive = new KeepAlive(this, webSocket);
            this.keepAlive.start();
        }

        @Override
        public void onClose(WebSocket webSocket, int code, String message) {
            LOGGER.error("Closed socket: " + message);
            if (this.keepAlive != null) {
                this.keepAlive.stop();
            }
            if (code != SessionStopReason.NORMAL_CLOSE.code) {
                collar.configuration.listener.onError(collar, message);
            }
            if (state != State.DISCONNECTED) {
                collar.changeState(State.DISCONNECTED);
            }
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable throwable) {
            LOGGER.error("Socket failure", throwable);
            if (state != State.DISCONNECTED) {
                collar.changeState(State.DISCONNECTED);
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteBuffer messageBuffer) {
            readResponse(messageBuffer).ifPresent(resp -> {
                if (resp instanceof IdentifyResponse) {
                    ServerIdentity storedServerIdentity = identityStore.serverIdentity();
                    IdentifyResponse response = (IdentifyResponse) resp;
                    if (!response.identity.equals(storedServerIdentity)) {
                        configuration.listener.onClientUntrusted(collar, identityStore);
                        disconnect();
                        return;
                    }
                    if (!identityStore.verifyIdentityResponse(response)) {
                        configuration.listener.onClientUntrusted(collar, identityStore);
                        disconnect();
                        return;
                    }
                    MinecraftSession session = configuration.sessionSupplier.get();
                    String serverId;
                    if (session.mode == MinecraftSession.Mode.MOJANG) {
                        Mojang authentication = new Mojang(Http.client());
                        Optional<Mojang.JoinServerResponse> joinServerResponse = authentication.joinServer(session, response.minecraftServerId, response.minecraftSharedSecret);
                        if (joinServerResponse.isPresent()) {
                            serverId = joinServerResponse.get().serverId;
                        } else {
                            throw new ConnectionException("Couldn't verify your client session with Mojang");
                        }
                    } else {
                        serverId = null;
                    }
                    this.serverIdentity = response.identity;
                    sendRequest(webSocket, new StartSessionRequest(session, serverId));
                    keepAlive.stop();
                    keepAlive.start();
                } else if (resp instanceof KeepAliveResponse) {
                    LOGGER.trace("KeepAliveResponse received");
                } else if (resp instanceof RegisterDeviceResponse) {
                    RegisterDeviceResponse registerDeviceResponse = (RegisterDeviceResponse) resp;
                    LOGGER.info("RegisterDeviceResponse received with registration url " + ((RegisterDeviceResponse) resp).approvalUrl);
                    configuration.listener.onConfirmDeviceRegistration(collar, registerDeviceResponse.approvalToken, registerDeviceResponse.approvalUrl);
                } else if (resp instanceof DeviceRegisteredResponse) {
                    DeviceRegisteredResponse response = (DeviceRegisteredResponse) resp;
                    sendRequest(webSocket, identityStore.processDeviceRegisteredResponse(response));
                } else if (resp instanceof StartSessionResponse) {
                    LOGGER.info("Session has started");
                    collar.changeState(State.CONNECTED);
                } else if (resp instanceof SessionFailedResponse) {
                    LOGGER.info("SessionFailedResponse received");
                    if (resp instanceof MojangVerificationFailedResponse) {
                        MojangVerificationFailedResponse response = (MojangVerificationFailedResponse) resp;
                        LOGGER.info("SessionFailedResponse with mojang session verification failure");
                        configuration.listener.onMinecraftAccountVerificationFailed(collar, response.minecraftSession);
                    } else if (resp instanceof PrivateIdentityMismatchResponse) {
                        PrivateIdentityMismatchResponse response = (PrivateIdentityMismatchResponse) resp;
                        LOGGER.info("SessionFailedResponse with private identity mismatch");
                        configuration.listener.onPrivateIdentityMismatch(collar, response.url);
                    } else if (resp instanceof SessionErrorResponse) {
                        SessionErrorResponse response = (SessionErrorResponse) resp;
                        String message = response.reason.message(response.message);
                        LOGGER.info("SessionFailedResponse Reason: " + message);
                        if (response.reason != SessionStopReason.NORMAL_CLOSE) {
                            collar.configuration.listener.onError(collar, response.reason.message(response.message));
                        }
                    }
                    collar.changeState(State.DISCONNECTED);
                } else {
                    for (AbstractApi<?> api : collar.apis) {
                        if (api.handleResponse(resp)) {
                            break;
                        }
                    }
                }
            });
        }

        private Optional<ProtocolResponse> readResponse(ByteBuffer buffer) {
            try {
                return packets().decode(serverIdentity, buffer, ProtocolResponse.class);
            } catch (CipherException e) {
                throw new IllegalStateException("Could not recover from cipher error", e);
            } catch (IOException e) {
                throw new IllegalStateException("Read error ", e);
            }
        }

        public void sendRequest(WebSocket webSocket, ProtocolRequest req) {
            PacketIO packetIO = packets();
            byte[] bytes;
            if (state == State.CONNECTED) {
                try {
                    if (identityStore == null) {
                        throw new IllegalStateException("identity store should be available by the time the client is " + State.CONNECTED);
                    }
                    bytes = packets().encodeEncrypted(serverIdentity, req);
                } catch (InvalidCipherSessionException e) {
                    collar.configuration.listener.onClientUntrusted(collar, identityStore);
                    return;
                } catch (IOException | CipherException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                try {
                    bytes = packetIO.encodePlain(req);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            webSocket.send(ByteBuffer.wrap(bytes));
        }

        private PacketIO packets() {
            return identityStore.isValid() ? new PacketIO(mapper, identityStore.cipher()) : new PacketIO(mapper, null);
        }
    }

    public enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    static {
        Crypto.removeCryptographyRestrictions();
    }
}
