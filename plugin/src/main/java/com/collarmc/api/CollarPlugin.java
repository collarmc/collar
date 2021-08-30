package com.collarmc.api;

import com.collarmc.client.Collar;

/**
 * Used for Minecraft mod's to hook into the collar core mod
 */
public interface CollarPlugin {
    /**
     * Called when the collar plugin is loaded
     * If collar's core mod is not enabled, this function will not be called.
     * @param event of load
     */
    default void onLoad(CollarPluginLoadedEvent event) {}

    /**
     * When the client is connecting
     * @param collar client instance
     */
    default void onConnecting(Collar collar) {}

    /**
     * When the client is connected
     * @param collar client instance
     */
    default void onConnected(Collar collar) {}

    /**
     * When the client is disconnected
     * @param collar client instance
     */
    default void onDisconnected(Collar collar) {}
}
