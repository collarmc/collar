package com.collarmc.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.collarmc.http.WebSocket;
import com.collarmc.protocol.keepalive.KeepAliveRequest;
import com.collarmc.api.identity.ClientIdentity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Schedules a {@link KeepAliveRequest} for the connection lifecycle of a {@link WebSocket}
 */
final class KeepAlive {
    private static final Logger LOGGER = LogManager.getLogger(KeepAlive.class.getName());
    private final WebSocket webSocket;
    private ScheduledExecutorService scheduler;
    private final Collar.CollarWebSocket collarWebSocket;

    public KeepAlive(Collar.CollarWebSocket collarWebSocket, WebSocket webSocket) {
        this.collarWebSocket = collarWebSocket;
        this.webSocket = webSocket;
    }

    public void start(ClientIdentity identity) {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                KeepAliveRequest keepAliveRequest = new KeepAliveRequest(identity);
                collarWebSocket.sendRequest(webSocket, keepAliveRequest);
            } catch (CollarException.ConnectionException e) {
                LOGGER.error("Couldn't send KeepAliveRequest", e);
                webSocket.close(1000, "Keep alive failed");
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public void stop() {
        if (this.scheduler != null) {
            this.scheduler.shutdown();
        }
    }
}
