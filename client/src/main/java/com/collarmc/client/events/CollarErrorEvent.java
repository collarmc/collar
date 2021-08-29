package com.collarmc.client.events;

import com.collarmc.client.Collar;

/**
 * Fired whenever Collar encounters an unrecoverable error
 */
public final class CollarErrorEvent extends AbstractCollarEvent {
    public final String message;
    public final Throwable error;

    public CollarErrorEvent(Collar collar, String message, Throwable error) {
        super(collar);
        this.message = message;
        this.error = error;
    }
}
