package com.collarmc.protocol.waypoints;

import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.security.ClientIdentity;

import java.util.UUID;

public final class CreateWaypointRequest extends ProtocolRequest {
    @JsonProperty("waypointId")
    public final UUID waypointId;
    @JsonProperty("waypoint")
    public final byte[] waypoint;

    public CreateWaypointRequest(@JsonProperty("identity") ClientIdentity identity,
                                 @JsonProperty("waypointId") UUID waypointId,
                                 @JsonProperty("waypoint") byte[] waypoint) {
        super(identity);
        this.waypointId = waypointId;
        this.waypoint = waypoint;
    }
}
