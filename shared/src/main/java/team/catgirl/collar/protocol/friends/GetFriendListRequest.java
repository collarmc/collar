package team.catgirl.collar.protocol.friends;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

public final class GetFriendListRequest extends ProtocolRequest {
    public GetFriendListRequest(@JsonProperty("identity") ClientIdentity identity) {
        super(identity);
    }
}
