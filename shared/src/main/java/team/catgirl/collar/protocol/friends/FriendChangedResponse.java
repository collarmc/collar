package team.catgirl.collar.protocol.friends;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.friends.Friend;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

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
