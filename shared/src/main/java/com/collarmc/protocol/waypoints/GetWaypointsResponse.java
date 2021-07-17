package com.collarmc.protocol.waypoints;

import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.security.ServerIdentity;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.api.waypoints.EncryptedWaypoint;

import java.util.List;

public final class GetWaypointsResponse extends ProtocolResponse {
    @JsonProperty("waypoints")
    public final List<EncryptedWaypoint> waypoints;

    public GetWaypointsResponse(
            @JsonProperty("identity") ServerIdentity identity,
            @JsonProperty("waypoints") List<EncryptedWaypoint> waypoints) {
        super(identity);
        this.waypoints = waypoints;
    }
}
