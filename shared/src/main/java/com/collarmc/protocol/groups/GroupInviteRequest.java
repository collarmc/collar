package com.collarmc.protocol.groups;

import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.api.identity.ClientIdentity;

import java.util.List;
import java.util.UUID;

/**
 * Send an invitation to players to join an existing group
 * Sent by the owner on behalf of all members of the group
 */
public final class GroupInviteRequest extends ProtocolRequest {
    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("players")
    public final List<UUID> players;

    @JsonCreator
    public GroupInviteRequest(
            @JsonProperty("identify") ClientIdentity identity,
            @JsonProperty("groupId") UUID groupId,
            @JsonProperty("players") List<UUID> players) {
        super(identity);
        this.groupId = groupId;
        this.players = players;
    }
}
