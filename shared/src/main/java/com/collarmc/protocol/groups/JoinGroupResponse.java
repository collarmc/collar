package com.collarmc.protocol.groups;

import com.collarmc.api.groups.Group;
import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.session.Player;
import com.collarmc.protocol.ProtocolResponse;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class JoinGroupResponse extends ProtocolResponse {
    @JsonProperty("group")
    public final Group group;
    @JsonProperty("sender")
    public final ClientIdentity sender;
    @JsonProperty("player")
    public final Player player;

    @JsonCreator
    public JoinGroupResponse(@JsonProperty("group") Group group,
                             @JsonProperty("sender") ClientIdentity sender,
                             @JsonProperty("player") Player player) {
        this.group = group;
        this.sender = sender;
        this.player = player;
    }
}
