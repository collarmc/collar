package com.collarmc.protocol.groups;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.session.Player;
import com.collarmc.protocol.ProtocolResponse;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Sent to all group members whenever someone leaves the group
 */
public final class LeaveGroupResponse extends ProtocolResponse {
    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("sender")
    public final ClientIdentity sender;
    @JsonProperty("player")
    public final Player player;

    @JsonCreator
    public LeaveGroupResponse(@JsonProperty("groupId") UUID groupId,
                              @JsonProperty("sender") ClientIdentity sender,
                              @JsonProperty("player") Player player) {
        this.groupId = groupId;
        this.sender = sender;
        this.player = player;
    }
}
