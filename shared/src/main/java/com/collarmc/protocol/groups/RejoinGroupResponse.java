package com.collarmc.protocol.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.security.ServerIdentity;

import java.util.UUID;

/**
 * Sent to the group member when they reconnect
 */
public final class RejoinGroupResponse extends ProtocolResponse {
    @JsonProperty("group")
    public final UUID group;

    public RejoinGroupResponse(@JsonProperty("identity") ServerIdentity identity, @JsonProperty("group") UUID group) {
        super(identity);
        this.group = group;
    }
}
