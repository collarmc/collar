package com.collarmc.protocol.friends;

import com.collarmc.api.friends.Friend;
import com.collarmc.protocol.ProtocolResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class FriendChangedResponse extends ProtocolResponse {
    @JsonProperty("friend")
    public final Friend friend;

    public FriendChangedResponse(@JsonProperty("friend") Friend friend) {
        this.friend = friend;
    }
}
