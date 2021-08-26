package com.collarmc.protocol.waypoints;

import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class RemoveWaypointRequest extends ProtocolRequest {
    @JsonProperty("waypointId")
    public final UUID waypointId;

    @JsonCreator
    public RemoveWaypointRequest(@JsonProperty("waypointId") UUID waypointId) {
        this.waypointId = waypointId;
    }
}
