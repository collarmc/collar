package com.collarmc.protocol.friends;

import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.security.ClientIdentity;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class RemoveFriendRequest extends ProtocolRequest {
    @JsonProperty("player")
    public final UUID player;
    @JsonProperty("profile")
    public final UUID profile;

    public RemoveFriendRequest(@JsonProperty("identity") ClientIdentity identity,
                               @JsonProperty("player") UUID player,
                               @JsonProperty("profile") UUID profile) {
        super(identity);
        this.player = player;
        this.profile = profile;
    }
}
