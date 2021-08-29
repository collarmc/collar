package com.collarmc.client.api.location.events;

import com.collarmc.api.groups.Group;
import com.collarmc.client.Collar;
import com.collarmc.client.events.AbstractCollarEvent;

/**
 * Fired when the player stops sharing their location
 */
public final class LocationSharingStoppedEvent extends AbstractCollarEvent {
    public final Group group;

    public LocationSharingStoppedEvent(Collar collar, Group group) {
        super(collar);
        this.group = group;
    }
}
