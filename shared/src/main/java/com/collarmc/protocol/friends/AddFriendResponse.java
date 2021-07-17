package com.collarmc.protocol.friends;

import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.security.ServerIdentity;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.api.friends.Friend;

public final class AddFriendResponse extends ProtocolResponse {
    @JsonProperty("friend")
    public final Friend friend;

    public AddFriendResponse(@JsonProperty("identity") ServerIdentity identity,
                             @JsonProperty("friend") Friend friend) {
        super(identity);
        this.friend = friend;
    }
}
