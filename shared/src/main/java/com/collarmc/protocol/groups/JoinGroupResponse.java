package com.collarmc.protocol.groups;

import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.security.ServerIdentity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.api.session.Player;
import com.collarmc.security.ClientIdentity;

import java.util.UUID;

public final class JoinGroupResponse extends ProtocolResponse {
    @JsonProperty("group")
    public final UUID group;
    @JsonProperty("sender")
    public final ClientIdentity sender;
    @JsonProperty("player")
    public final Player player;
    @JsonProperty("keys")
    public final byte[] keys;

    @JsonCreator
    public JoinGroupResponse(@JsonProperty("identity") ServerIdentity identity,
                             @JsonProperty("group") UUID group,
                             @JsonProperty("sender") ClientIdentity sender,
                             @JsonProperty("player") Player player,
                             @JsonProperty("keys") byte[] keys) {
        super(identity);
        this.group = group;
        this.sender = sender;
        this.player = player;
        this.keys = keys;
    }
}
