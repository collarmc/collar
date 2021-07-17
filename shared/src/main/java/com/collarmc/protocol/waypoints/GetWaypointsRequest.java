package com.collarmc.protocol.waypoints;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.security.ClientIdentity;

public final class GetWaypointsRequest extends ProtocolRequest {
    public GetWaypointsRequest(@JsonProperty("identity") ClientIdentity identity) {
        super(identity);
    }
}
