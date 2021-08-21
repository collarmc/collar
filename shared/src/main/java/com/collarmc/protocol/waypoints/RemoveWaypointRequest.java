package com.collarmc.protocol.waypoints;

import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.api.identity.ClientIdentity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class RemoveWaypointRequest extends ProtocolRequest {
    @JsonProperty("waypointId")
    public final UUID waypointId;

    @JsonCreator
    public RemoveWaypointRequest(
            @JsonProperty("identity") ClientIdentity identity,
            @JsonProperty("waypointId") UUID waypointId) {
        super(identity);
        this.waypointId = waypointId;
    }
}
