package com.collarmc.protocol.friends;

import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.api.identity.ServerIdentity;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.api.friends.Friend;

import java.util.List;

public final class GetFriendListResponse extends ProtocolResponse {
    @JsonProperty("friends")
    public final List<Friend> friends;

    public GetFriendListResponse(@JsonProperty("identity") ServerIdentity identity,
                                 @JsonProperty("friends") List<Friend> friends) {
        super(identity);
        this.friends = friends;
    }
}
