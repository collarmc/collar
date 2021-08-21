package com.collarmc.protocol.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.api.identity.ClientIdentity;

import java.util.UUID;

public final class DeleteGroupRequest extends ProtocolRequest {
    @JsonProperty("group")
    public final UUID group;

    public DeleteGroupRequest(@JsonProperty("identity") ClientIdentity identity, @JsonProperty("group") UUID group) {
        super(identity);
        this.group = group;
    }
}
