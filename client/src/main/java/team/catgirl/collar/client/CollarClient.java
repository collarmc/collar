package team.catgirl.collar.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.catgirl.collar.models.*;
import team.catgirl.collar.models.Group.MembershipState;
import team.catgirl.collar.messages.ClientMessage;
import team.catgirl.collar.messages.ClientMessage.*;
import team.catgirl.collar.messages.ServerMessage;
import team.catgirl.collar.utils.Utils;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CollarClient {

    private static final Logger LOGGER = Logger.getLogger(CollarClient.class.getName());

    private final ObjectMapper mapper = Utils.createObjectMapper();
    private final String baseUrl;
    private final OkHttpClient http;
    private Identity me;
    private WebSocket webSocket;
    private DelegatingListener listener;
    private boolean connected;
    private ScheduledExecutorService keepAliveScheduler;

    public CollarClient(String baseUrl) {
        if (Strings.isNullOrEmpty(baseUrl)) {
            throw new IllegalArgumentException("baseUrl was not set");
        }
        // Make sure we don't append the / twice otherwise jetty won't connect to server
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf('/'));
        }
        this.baseUrl = baseUrl + "/api/1/";
        this.http = new OkHttpClient();
    }

    public void connect(Identity identity, CollarListener listener) {
        if (this.connected) {
            throw new IllegalStateException("Is already connected");
        }
        this.listener = new DelegatingListener(listener); // TODO: wrap in delegating listener that catches and logs exceptions
        this.me = identity;
        Request request = new Request.Builder().url(baseUrl + "coordshare/listen").build();
        webSocket = http.newWebSocket(request, new WebSocketListenerImpl(this));
        http.dispatcher().executorService().shutdown();
        keepAliveScheduler = Executors.newScheduledThreadPool(1);
        keepAliveScheduler.scheduleAtFixedRate((Runnable) () -> {
            try {
                send(new ClientMessage(null, null, null, null, null, new Ping(), null));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Couldn't send ping");
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public void disconnect() {
        if (!this.connected) {
            throw new IllegalStateException("Is already disconnected");
        }
        webSocket.close(1000, "Disconnected");
        CollarListener listener = this.listener;
        this.listener = null;
        this.me = null;
        this.connected = false;
        listener.onDisconnect(this);
        this.keepAliveScheduler.shutdown();
        this.keepAliveScheduler = null;
    }

    public void reconnect() throws InterruptedException {
        Thread.sleep(TimeUnit.SECONDS.toMillis(30));
        Identity oldIdentity = me;
        CollarListener oldListener = listener;
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

    public Identity getMe() {
        return me;
    }

    public void createGroup(List<UUID> players, Position position) throws IOException {
        CreateGroupRequest req = new CreateGroupRequest(me, players, position);
        send(new ClientMessage(null, req, null, null, null, null, null));
    }

    public void acceptGroupRequest(String groupId, MembershipState state) throws IOException {
        send(new ClientMessage(null, null, new AcceptGroupMembershipRequest(me, groupId, state), null, null, null, null));
    }

    public void leaveGroup(Group group) throws IOException {
        send(new ClientMessage(null, null, null, new LeaveGroupRequest(me, group.id), null, null, null));
    }

    public void updatePosition(Position position) throws IOException {
        send(new ClientMessage(null, null, null, null, new UpdatePlayerStateRequest(me, position), null, null));
    }

    public void invite(Group group, List<UUID> players) throws IOException {
        send(new ClientMessage(null, null, null, null, null, null, new GroupInviteRequest(me, group.id, players)));
    }

    private void send(ClientMessage o) throws IOException {
        String message = mapper.writeValueAsString(o);
        webSocket.send(message);
    }

    class WebSocketListenerImpl extends WebSocketListener {

        private final CollarClient client;

        public WebSocketListenerImpl(CollarClient client) {
            this.client = client;
        }

        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            LOGGER.info("onOpen is called");
            super.onOpen(webSocket, response);
            ClientMessage message = new ClientMessage(new IdentifyRequest(me), null, null, null, null, null, null);
            try {
                send(message);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Could not send identity. Closing client", e);
                disconnect();
            }
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            ServerMessage message;
            try {
                message = mapper.readValue(text, ServerMessage.class);
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
            if (message.updatePlayerStateResponse != null) {
                listener.onGroupUpdated(client, message.updatePlayerStateResponse);
            }
            if (message.groupInviteResponse != null) {
                listener.onGroupInvitesSent(client, message.groupInviteResponse);
            }
            if (message.acceptGroupMembershipResponse != null) {
                listener.onGroupJoined(client, message.acceptGroupMembershipResponse);
            }
            if (message.pong != null) {
                listener.onPongReceived(message.pong);
            }
        }

        @Override
        public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            LOGGER.log(Level.INFO, "Connection closing...");
            disconnect();
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
            LOGGER.log(Level.SEVERE, "Communications failure", t);
        }

        @Override
        public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            super.onClosed(webSocket, code, reason);
            LOGGER.log(Level.SEVERE, "closed: " + reason);
        }
    }

    public class DelegatingListener implements CollarListener {
        private final CollarListener listener;

        public DelegatingListener(CollarListener listener) {
            this.listener = listener;
        }

        @Override
        public void onSessionCreated(CollarClient client) {
            listener.onSessionCreated(client);
            connected = true;
        }

        @Override
        public void onDisconnect(CollarClient client) {
            listener.onDisconnect(client);
        }

        @Override
        public void onGroupCreated(CollarClient client, ServerMessage.CreateGroupResponse resp) {
            listener.onGroupCreated(client, resp);
        }

        @Override
        public void onGroupMembershipRequested(CollarClient client, ServerMessage.GroupMembershipRequest resp) {
            listener.onGroupMembershipRequested(client, resp);
        }

        @Override
        public void onGroupJoined(CollarClient client, ServerMessage.AcceptGroupMembershipResponse acceptGroupMembershipResponse) {
            listener.onGroupJoined(client, acceptGroupMembershipResponse);
        }

        @Override
        public void onGroupLeft(CollarClient client, ServerMessage.LeaveGroupResponse resp) {
            listener.onGroupLeft(client, resp);
        }

        @Override
        public void onGroupUpdated(CollarClient client, ServerMessage.UpdatePlayerStateResponse updatePlayerStateResponse) {
            listener.onGroupUpdated(client, updatePlayerStateResponse);
        }

        @Override
        public void onPongReceived(ServerMessage.Pong pong) {
            listener.onPongReceived(pong);
            LOGGER.log(Level.FINE, "Received ping");
        }

        @Override
        public void onGroupInvitesSent(CollarClient client, ServerMessage.GroupInviteResponse resp) {
            listener.onGroupInvitesSent(client, resp);
        }
    }
}
