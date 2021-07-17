package com.collarmc.server.protocol;

import org.eclipse.jetty.websocket.api.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.collarmc.api.session.Player;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.protocol.messaging.SendMessageRequest;
import com.collarmc.protocol.messaging.SendMessageResponse;
import com.collarmc.security.ClientIdentity;
import com.collarmc.security.ServerIdentity;
import com.collarmc.server.CollarServer;
import com.collarmc.server.services.groups.GroupService;
import com.collarmc.server.session.SessionManager;

import java.util.function.BiConsumer;

public class MessagingProtocolHandler extends ProtocolHandler {

    private static final Logger LOGGER = LogManager.getLogger(MessagingProtocolHandler.class.getName());

    private final SessionManager sessions;
    private final GroupService groups;
    private final ServerIdentity serverIdentity;

    public MessagingProtocolHandler(SessionManager sessions, GroupService groups, ServerIdentity serverIdentity) {
        this.sessions = sessions;
        this.groups = groups;
        this.serverIdentity = serverIdentity;
    }

    @Override
    public boolean handleRequest(CollarServer collar, ProtocolRequest req, BiConsumer<ClientIdentity, ProtocolResponse> sender) {
        if (req instanceof SendMessageRequest) {
            SendMessageRequest request = (SendMessageRequest) req;
            if (request.group != null) {
                groups.createMessages(request).ifPresent(response -> sender.accept(null, response));
            } else if (request.recipient != null) {
                sessions.findPlayer(request.identity).ifPresentOrElse(player -> {
                    sender.accept(request.recipient, new SendMessageResponse(this.serverIdentity, req.identity, null, player, request.message));
                }, () -> {
                    LOGGER.info("could not find player for " + req.identity);
                });
            } else {
                LOGGER.warn( "sent a malformed SendMessageRequest by " + req.identity);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onSessionStopping(ClientIdentity identity, Player player, BiConsumer<Session, ProtocolResponse> sender) {}
}
