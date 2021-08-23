package com.collarmc.protocol.waypoints;

import com.collarmc.api.waypoints.EncryptedWaypoint;
import com.collarmc.protocol.ProtocolResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class GetWaypointsResponse extends ProtocolResponse {
    @JsonProperty("waypoints")
    public final List<EncryptedWaypoint> waypoints;

    public GetWaypointsResponse(@JsonProperty("waypoints") List<EncryptedWaypoint> waypoints) {
        this.waypoints = waypoints;
    }
}
