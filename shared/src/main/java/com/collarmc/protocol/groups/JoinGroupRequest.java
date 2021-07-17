package com.collarmc.protocol.groups;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.api.groups.MembershipState;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.security.ClientIdentity;

import java.util.UUID;

/**
 * Accept an invitation to the group
 */
public final class JoinGroupRequest extends ProtocolRequest {
    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("state")
    public final MembershipState state;
    @JsonProperty("keys")
    public final byte[] keys;

    @JsonCreator
    public JoinGroupRequest(
            @JsonProperty("identity") ClientIdentity identity,
            @JsonProperty("groupId") UUID groupId,
            @JsonProperty("state") MembershipState state,
            @JsonProperty("keys") byte[] keys) {
        super(identity);
        this.groupId = groupId;
        this.state = state;
        this.keys = keys;
    }
}
