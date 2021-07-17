package com.collarmc.protocol.friends;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.security.ClientIdentity;

public final class GetFriendListRequest extends ProtocolRequest {
    public GetFriendListRequest(@JsonProperty("identity") ClientIdentity identity) {
        super(identity);
    }
}
