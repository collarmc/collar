package com.collarmc.protocol.friends;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.api.identity.ClientIdentity;

import java.util.UUID;

public final class AddFriendRequest extends ProtocolRequest {
    @JsonProperty("player")
    public final UUID player;
    @JsonProperty("profile")
    public final UUID profile;

    public AddFriendRequest(@JsonProperty("identity") ClientIdentity identity,
                            @JsonProperty("player") UUID player,
                            @JsonProperty("profile") UUID profile) {
        super(identity);
        this.player = player;
        this.profile = profile;
    }
}
