package com.collarmc.client.api.plugin;

import com.collarmc.pounce.EventBus;

/**
 * Fired when {@link CollarPlugin#onLoad(CollarPluginLoadedEvent)}
 */
public final class CollarPluginLoadedEvent {
    /**
     * Event bus for publishing all events to
     */
    public final EventBus eventBus;

    public CollarPluginLoadedEvent(EventBus eventBus) {
        this.eventBus = eventBus;
    }
}
