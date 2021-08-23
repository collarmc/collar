package com.collarmc.protocol.groups;

import com.collarmc.protocol.ProtocolResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Sent to the group member when they reconnect
 */
public final class RejoinGroupResponse extends ProtocolResponse {
    @JsonProperty("group")
    public final UUID group;

    public RejoinGroupResponse(@JsonProperty("group") UUID group) {
        this.group = group;
    }
}
