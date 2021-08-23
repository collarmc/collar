package com.collarmc.protocol.groups;

import com.collarmc.api.groups.GroupType;
import com.collarmc.api.session.Player;
import com.collarmc.protocol.ProtocolResponse;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Sent by the group owner to invite players to an existing group using a {@link GroupInviteRequest}
 */
public final class GroupInviteResponse extends ProtocolResponse {
    @JsonProperty("group")
    public final UUID group;
    @JsonProperty("name")
    public final String name;
    @JsonProperty("type")
    public final GroupType type;
    @JsonProperty("sender")
    public final Player sender;

    @JsonCreator
    public GroupInviteResponse(
            @JsonProperty("group") UUID group,
            @JsonProperty("name") String name,
            @JsonProperty("type") GroupType type,
            @JsonProperty("sender") Player sender) {
        this.group = group;
        this.name = name;
        this.type = type;
        this.sender = sender;
    }
}
