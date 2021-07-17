package com.collarmc.client.api.location;

import com.collarmc.api.groups.Group;
import com.collarmc.api.location.Location;
import com.collarmc.api.session.Player;
import com.collarmc.api.waypoints.Waypoint;
import com.collarmc.client.Collar;
import com.collarmc.client.api.ApiListener;

import java.util.Set;

public interface LocationListener extends ApiListener {
    /**
     * Fired when a player location was updated
     * @param collar client
     * @param locationApi api
     * @param player that is sharing their location
     * @param location of the player
     */
    default void onLocationUpdated(Collar collar, LocationApi locationApi, Player player, Location location) {}

    /**
     * Fired when a waypoint was created
     * @param collar client
     * @param locationApi api
     * @param group the waypoint belong to or null if private waypoint
     * @param waypoint that was removed
     */
    default void onWaypointCreated(Collar collar, LocationApi locationApi, Group group, Waypoint waypoint) {}

    /**
     * Fired when a waypoint was removed
     * @param collar client
     * @param locationApi api
     * @param group the waypoint belong to or null if private waypoint
     * @param waypoint that was removed
     */
    default void onWaypointRemoved(Collar collar, LocationApi locationApi, Group group, Waypoint waypoint) {}

    /**
     * Fired when private waypoints are received from the server.
     * @param collar client
     * @param locationApi api
     * @param privateWaypoints received
     */
    default void onPrivateWaypointsReceived(Collar collar, LocationApi locationApi, Set<Waypoint> privateWaypoints) {}

    /**
     * Fired when the player starts sharing their location
     * @param collar client
     * @param locationApi api
     * @param group location being shared with
     */
    default void onStartedSharingLocation(Collar collar, LocationApi locationApi, Group group) {}

    /**
     * Fired when the player stops sharing their location
     * @param collar client
     * @param locationApi api
     * @param group location being shared with
     */
    default void onStoppedSharingLocation(Collar collar, LocationApi locationApi, Group group) {}
}
