package com.collarmc.client.api.location.events;

import com.collarmc.api.waypoints.Waypoint;
import com.collarmc.client.Collar;
import com.collarmc.client.events.AbstractCollarEvent;

import java.util.Set;

/**
 * Fired when private waypoints are received from the server.
 */
public final class PrivateWaypointsReceivedEvent extends AbstractCollarEvent {
    public final Set<Waypoint> privateWaypoints;

    public PrivateWaypointsReceivedEvent(Collar collar, Set<Waypoint> privateWaypoints) {
        super(collar);
        this.privateWaypoints = privateWaypoints;
    }
}
