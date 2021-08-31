package com.collarmc.protocol.messaging;

import com.collarmc.api.identity.Identity;
import com.collarmc.api.profiles.PublicProfile;
import com.collarmc.api.session.Player;
import com.collarmc.protocol.ProtocolResponse;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class SendMessageResponse extends ProtocolResponse {
    @JsonProperty("sender")
    public final Identity sender;
    @JsonProperty("group")
    public final UUID group;
    @JsonProperty("player")
    public final Player player;
    @JsonProperty("profile")
    public final PublicProfile profile;
    @JsonProperty("message")
    public final byte[] message;

    @JsonCreator
    public SendMessageResponse(@JsonProperty("sender") Identity sender,
                               @JsonProperty("group") UUID group,
                               @JsonProperty("player") Player player,
                               @JsonProperty("profile") PublicProfile profile,
                               @JsonProperty("message") byte[] message) {
        this.sender = sender;
        this.group = group;
        this.player = player;
        this.profile = profile;
        this.message = message;
    }
}
