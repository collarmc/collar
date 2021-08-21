package com.collarmc.protocol.messaging;

import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.api.identity.Identity;
import com.collarmc.api.identity.ServerIdentity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.api.session.Player;

import java.util.UUID;

public final class SendMessageResponse extends ProtocolResponse {
    @JsonProperty("sender")
    public final Identity sender;
    @JsonProperty("group")
    public final UUID group;
    @JsonProperty("player")
    public final Player player;
    @JsonProperty("message")
    public final byte[] message;

    @JsonCreator
    public SendMessageResponse(@JsonProperty("identity") ServerIdentity identity,
                               @JsonProperty("sender") Identity sender,
                               @JsonProperty("group") UUID group,
                               @JsonProperty("player") Player player,
                               @JsonProperty("message") byte[] message) {
        super(identity);
        this.sender = sender;
        this.group = group;
        this.player = player;
        this.message = message;
    }
}
