package com.collarmc.protocol.friends;

import com.collarmc.api.friends.Friend;
import com.collarmc.protocol.ProtocolResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class GetFriendListResponse extends ProtocolResponse {
    @JsonProperty("friends")
    public final List<Friend> friends;

    public GetFriendListResponse(@JsonProperty("friends") List<Friend> friends) {
        this.friends = friends;
    }
}
