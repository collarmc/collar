package com.collarmc.protocol.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.security.ClientIdentity;

import java.util.UUID;

public final class StartSharingLocationRequest extends ProtocolRequest {
    @JsonProperty("groupId")
    public final UUID groupId;

    public StartSharingLocationRequest(@JsonProperty("identity") ClientIdentity identity, @JsonProperty("groupId") UUID groupId) {
        super(identity);
        this.groupId = groupId;
    }
}
