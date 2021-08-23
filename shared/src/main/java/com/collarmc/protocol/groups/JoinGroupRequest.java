package com.collarmc.protocol.groups;

import com.collarmc.api.groups.MembershipState;
import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Accept an invitation to the group
 */
public final class JoinGroupRequest extends ProtocolRequest {
    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("state")
    public final MembershipState state;

    @JsonCreator
    public JoinGroupRequest(
            @JsonProperty("groupId") UUID groupId,
            @JsonProperty("state") MembershipState state) {
        this.groupId = groupId;
        this.state = state;
    }
}
