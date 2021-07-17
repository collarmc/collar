package com.collarmc.protocol.friends;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.api.friends.Friend;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.security.ServerIdentity;

public final class FriendChangedResponse extends ProtocolResponse {
    @JsonProperty("friend")
    public final Friend friend;

    public FriendChangedResponse(
            @JsonProperty("identity") ServerIdentity identity,
            @JsonProperty("friend") Friend friend) {
        super(identity);
        this.friend = friend;
    }
}
