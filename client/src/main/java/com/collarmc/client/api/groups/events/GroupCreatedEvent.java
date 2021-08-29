package com.collarmc.client.api.groups.events;

import com.collarmc.api.groups.Group;
import com.collarmc.client.Collar;
import com.collarmc.client.events.AbstractCollarEvent;

/**
 * Fired when a group is created
 */
public final class GroupCreatedEvent extends AbstractCollarEvent {
    public final Group group;

    public GroupCreatedEvent(Collar collar, Group group) {
        super(collar);
        this.group = group;
    }
}
