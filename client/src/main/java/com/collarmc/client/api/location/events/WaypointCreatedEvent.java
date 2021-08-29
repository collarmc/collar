package com.collarmc.client.api.location.events;

import com.collarmc.api.groups.Group;
import com.collarmc.api.waypoints.Waypoint;
import com.collarmc.client.Collar;
import com.collarmc.client.events.AbstractCollarEvent;

/**
 * Fired when a waypoint was created
 */
public final class WaypointCreatedEvent extends AbstractCollarEvent {
    public final Waypoint waypoint;
    public final Group group;

    public WaypointCreatedEvent(Collar collar, Waypoint waypoint, Group group) {
        super(collar);
        this.waypoint = waypoint;
        this.group = group;
    }
}
