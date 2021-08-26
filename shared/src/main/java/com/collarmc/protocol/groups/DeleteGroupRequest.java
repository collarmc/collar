package com.collarmc.protocol.groups;

import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class DeleteGroupRequest extends ProtocolRequest {
    @JsonProperty("group")
    public final UUID group;

    public DeleteGroupRequest(@JsonProperty("group") UUID group) {
        this.group = group;
    }
}
