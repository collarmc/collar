package com.collarmc.protocol.friends;

import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class AddFriendRequest extends ProtocolRequest {
    @JsonProperty("player")
    public final UUID player;
    @JsonProperty("profile")
    public final UUID profile;

    public AddFriendRequest(@JsonProperty("player") UUID player,
                            @JsonProperty("profile") UUID profile) {
        this.player = player;
        this.profile = profile;
    }
}
