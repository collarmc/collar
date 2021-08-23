package com.collarmc.protocol.location;

import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Sent by a client when it wants to stop sharing its location
 */
public final class StopSharingLocationRequest extends ProtocolRequest {
    @JsonProperty("groupId")
    public final UUID groupId;

    public StopSharingLocationRequest(@JsonProperty("groupId") UUID groupId) {
        this.groupId = groupId;
    }
}
