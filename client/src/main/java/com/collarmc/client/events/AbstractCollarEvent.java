package com.collarmc.client.events;

import com.collarmc.client.Collar;

/**
 * Base type for all Collar events
 */
public abstract class AbstractCollarEvent {
    public final Collar collar;

    public AbstractCollarEvent(Collar collar) {
        this.collar = collar;
    }
}
