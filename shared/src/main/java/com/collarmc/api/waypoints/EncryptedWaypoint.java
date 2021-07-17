package com.collarmc.api.waypoints;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Encrypted {@link Waypoint}
 */
public final class EncryptedWaypoint {
    /**
     * The ID of the waypoint
     */
    @JsonProperty("waypointId")
    public final UUID waypointId;

    /**
     * The encrypted {@link Waypoint} data
     */
    @JsonProperty("waypoint")
    public final byte[] waypoint;

    public EncryptedWaypoint(@JsonProperty("waypointId") UUID waypointId,
                             @JsonProperty("waypoint") byte[] waypoint) {
        this.waypointId = waypointId;
        this.waypoint = waypoint;
    }
}
