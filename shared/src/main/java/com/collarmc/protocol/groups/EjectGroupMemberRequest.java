package com.collarmc.protocol.groups;

import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Sent by the group owner to forcefully remove a player from the group
 * Sender and all members recieve a {@link LeaveGroupResponse}
 */
public final class EjectGroupMemberRequest extends ProtocolRequest {

    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("player")
    public final UUID player;

    @JsonCreator
    public EjectGroupMemberRequest(@JsonProperty("groupId") UUID groupId,
                                   @JsonProperty("player") UUID player) {
        this.groupId = groupId;
        this.player = player;
    }
}
