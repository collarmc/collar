package com.collarmc.client.events;

import com.collarmc.client.Collar;
import com.collarmc.client.Collar.State;

/**
 * Fired when the state of the client changes
 */
public final class CollarStateChangedEvent extends AbstractCollarEvent {
    public final State state;

    public CollarStateChangedEvent(Collar collar, State state) {
        super(collar);
        this.state = state;
    }
}
