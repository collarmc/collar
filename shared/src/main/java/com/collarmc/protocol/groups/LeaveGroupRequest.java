package com.collarmc.protocol.groups;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.security.ClientIdentity;

import java.util.UUID;

/**
 * Sent by the client to leave a group on their own accord
 */
public final class LeaveGroupRequest extends ProtocolRequest {
    @JsonProperty("groupId")
    public final UUID groupId;

    @JsonCreator
    public LeaveGroupRequest(@JsonProperty("identity") ClientIdentity identity, @JsonProperty("groupId") UUID groupId) {
        super(identity);
        this.groupId = groupId;
    }
}
