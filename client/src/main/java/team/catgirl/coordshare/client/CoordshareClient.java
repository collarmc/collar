package team.catgirl.coordshare.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.catgirl.coordshare.models.*;
import team.catgirl.coordshare.models.Group.MembershipState;
import team.catgirl.coordshare.models.CoordshareClientMessage;
import team.catgirl.coordshare.models.CoordshareClientMessage.*;
import team.catgirl.coordshare.models.CoordshareServerMessage;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CoordshareClient {

    private static final Logger LOGGER = Logger.getLogger(CoordshareClient.class.getName());

    private final ObjectMapper mapper = Utils.createObjectMapper();
    private final String baseUrl;
    private final OkHttpClient http;
    private Identity me;
    private WebSocket webSocket;
    private CoordshareListener listener;
    private boolean connected;

    public CoordshareClient(String baseUrl) {
        this.baseUrl = baseUrl + "/api/1/";
        this.http = new OkHttpClient();
    }

    public void connect(Identity identity, CoordshareListener listener) {
        if (this.connected) {
            throw new IllegalStateException("Is already connected");
        }
        this.listener = listener; // TODO: wrap in delegating listener that catches and logs exceptions
        this.me = identity;
        Request request = new Request.Builder().url(baseUrl + "/coordshare/listen").build();
        webSocket = http.newWebSocket(request, new WebSocketListenerImpl(this));
        this.connected = true;
        listener.onConnected(this);
    }

    public void disconnect() {
        if (!this.connected) {
            throw new IllegalStateException("Is already disconnected");
        }
        webSocket.close(1000, "Disconnected");
        CoordshareListener listener = this.listener;
        this.listener = null;
        this.me = null;
        this.connected = false;
        listener.onDisconnect(this);
    }

    public void reconnect() throws InterruptedException {
        Thread.sleep(TimeUnit.SECONDS.toMillis(30));
        Identity oldIdentity = me;
        CoordshareListener oldListener = listener;
        disconnect();
        connect(oldIdentity, oldListener);
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isServerUp() {
        Request request = new Request.Builder()
                .url(baseUrl + "/status")
                .build();
        try (Response response = http.newCall(request).execute()) {
            return response.code() == 200;
        } catch (IOException ignored) {
            return false;
        }
    }

    public void createGroup(List<UUID> players, Position position) throws IOException {
        CreateGroupRequest req = new CreateGroupRequest(me, players, position);
        send(new CoordshareClientMessage(null, req, null, null, null));
    }

    public void acceptGroupRequest(String groupId, MembershipState state) throws IOException {
        send(new CoordshareClientMessage(null, null, new AcceptGroupMembershipRequest(me, groupId, state), null, null));
    }

    public void leaveGroup(String groupId) throws IOException {
        send(new CoordshareClientMessage(null, null, null, new LeaveGroupRequest(me, groupId), null));
    }

    public void updatePosition(Position position) throws IOException {
        send(new CoordshareClientMessage(null, null, null, null, new UpdatePositionRequest(me, position)));
    }

    private void send(CoordshareClientMessage o) throws IOException {
        String message = mapper.writeValueAsString(o);
        webSocket.send(message);
    }

    class WebSocketListenerImpl extends WebSocketListener {

        private final CoordshareClient client;

        public WebSocketListenerImpl(CoordshareClient client) {
            this.client = client;
        }

        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            super.onOpen(webSocket, response);
            CoordshareClientMessage message = new CoordshareClientMessage(new IdentifyRequest(me), null, null, null, null);
            try {
                send(message);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Could not send identity. Closing client", e);
                disconnect();
            }
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            CoordshareServerMessage message;
            try {
                message = mapper.readValue(text, CoordshareServerMessage.class);
            } catch (JsonProcessingException e) {
                LOGGER.log(Level.SEVERE, "Couldn't parse server message", e);
                disconnect();
                return;
            }
            if (message.identificationSuccessful != null) {
                listener.onSessionCreated(client);
            }
            if (message.groupMembershipRequest != null) {
                listener.onGroupMembershipRequested(client, message.groupMembershipRequest);
            }
            if (message.createGroupResponse != null) {
                listener.onGroupCreated(client, message.createGroupResponse);
            }
            if (message.leaveGroupResponse != null) {
                listener.onGroupLeft(client, message.leaveGroupResponse);
            }
            if (message.updatePositionResponse != null) {
                listener.onPositionsUpdated(client, message.updatePositionResponse);
            }
        }

        @Override
        public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            LOGGER.log(Level.INFO, "Connection closing...");
            disconnect();
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
            LOGGER.log(Level.SEVERE, "Communications failure. Reconnecting...", t);
            try {
                reconnect();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
