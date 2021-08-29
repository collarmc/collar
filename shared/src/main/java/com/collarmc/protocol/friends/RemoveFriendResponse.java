package com.collarmc.protocol.friends;

import com.collarmc.api.profiles.PublicProfile;
import com.collarmc.protocol.ProtocolResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class RemoveFriendResponse extends ProtocolResponse {
    @JsonProperty("friend")
    public final PublicProfile friend;

    public RemoveFriendResponse(@JsonProperty("friend") PublicProfile friend) {
        this.friend = friend;
    }
}
