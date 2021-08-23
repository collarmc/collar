package com.collarmc.protocol.location;

import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class StartSharingLocationRequest extends ProtocolRequest {
    @JsonProperty("groupId")
    public final UUID groupId;

    public StartSharingLocationRequest(@JsonProperty("groupId") UUID groupId) {
        this.groupId = groupId;
    }
}
