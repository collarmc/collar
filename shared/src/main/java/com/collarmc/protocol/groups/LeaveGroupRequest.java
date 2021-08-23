package com.collarmc.protocol.groups;

import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Sent by the client to leave a group on their own accord
 */
public final class LeaveGroupRequest extends ProtocolRequest {
    @JsonProperty("groupId")
    public final UUID groupId;

    @JsonCreator
    public LeaveGroupRequest(@JsonProperty("groupId") UUID groupId) {
        this.groupId = groupId;
    }
}
