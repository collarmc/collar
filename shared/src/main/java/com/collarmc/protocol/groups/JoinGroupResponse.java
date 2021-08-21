package com.collarmc.protocol.groups;

import com.collarmc.api.groups.Group;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.api.identity.ServerIdentity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.api.session.Player;
import com.collarmc.api.identity.ClientIdentity;

public final class JoinGroupResponse extends ProtocolResponse {
    @JsonProperty("group")
    public final Group group;
    @JsonProperty("sender")
    public final ClientIdentity sender;
    @JsonProperty("player")
    public final Player player;

    @JsonCreator
    public JoinGroupResponse(@JsonProperty("identity") ServerIdentity identity,
                             @JsonProperty("group") Group group,
                             @JsonProperty("sender") ClientIdentity sender,
                             @JsonProperty("player") Player player) {
        super(identity);
        this.group = group;
        this.sender = sender;
        this.player = player;
    }
}
