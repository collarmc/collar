package com.collarmc.protocol.friends;

import com.collarmc.protocol.ProtocolResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class RemoveFriendResponse extends ProtocolResponse {
    @JsonProperty("friend")
    public final UUID friend;

    public RemoveFriendResponse(
            @JsonProperty("friend") UUID friend) {
        this.friend = friend;
    }
}
