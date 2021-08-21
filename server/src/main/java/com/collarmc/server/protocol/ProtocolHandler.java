package com.collarmc.server.protocol;

import com.collarmc.server.session.SessionManager;
import org.eclipse.jetty.websocket.api.Session;
import com.collarmc.api.session.Player;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.server.CollarServer;

import java.util.function.BiConsumer;

/**
 * Extensible protocol packet listener and sender
 */
public abstract class ProtocolHandler {
    /**
     * Handles a request coming from a client and processes it
     * @param collar server
     * @param req request received
     * @param sender to send a response
     * @return if packet handled
     */
    public abstract boolean handleRequest(CollarServer collar, ProtocolRequest req, BiConsumer<ClientIdentity, ProtocolResponse> sender);

    /**
     * Fired when the session has started and all the session information is available
     * @param identity for which the session was started
     * @param player for which the session was started
     * @param sender to send responses to clients
     */
    public void onSessionStarted(ClientIdentity identity, Player player, BiConsumer<Session, ProtocolResponse> sender) {}

    /**
     * Fired before the session is stopped and all session state information is removed from {@link SessionManager}
     * @param identity for which the session is being stopped
     * @param player for which the sesssion is being stopped
     * @param sender to send responses to clients
     */
    public void onSessionStopping(ClientIdentity identity, Player player, BiConsumer<Session, ProtocolResponse> sender) {}
}
