package team.catgirl.collar.protocol.friends;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.friends.Friend;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

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
