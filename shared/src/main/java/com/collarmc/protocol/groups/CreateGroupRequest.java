package com.collarmc.protocol.groups;

import com.collarmc.api.groups.GroupType;
import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

/**
 * Create a new group and send {@link GroupInviteRequest}'s for all players in `players`
 */
public final class CreateGroupRequest extends ProtocolRequest {
    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("name")
    public final String name;
    @JsonProperty("type")
    public final GroupType type;
    @JsonProperty("players")
    public final List<UUID> players;

    @JsonCreator
    public CreateGroupRequest(@JsonProperty("groupId") UUID groupId,
                              @JsonProperty("name") String name,
                              @JsonProperty("type") GroupType type,
                              @JsonProperty("players") List<UUID> players) {
        this.groupId = groupId;
        this.name = name;
        this.type = type;
        this.players = players;
    }
}
