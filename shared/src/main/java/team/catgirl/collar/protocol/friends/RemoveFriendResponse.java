package team.catgirl.collar.protocol.friends;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

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
