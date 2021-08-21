package com.collarmc.protocol.friends;

import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.api.identity.ServerIdentity;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class RemoveFriendResponse extends ProtocolResponse {
    @JsonProperty("friend")
    public final UUID friend;

    public RemoveFriendResponse(
            @JsonProperty("identity") ServerIdentity identity,
            @JsonProperty("friend") UUID friend) {
        super(identity);
        this.friend = friend;
    }
}
