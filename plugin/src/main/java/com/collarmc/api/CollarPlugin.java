package com.collarmc.api;

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
}
