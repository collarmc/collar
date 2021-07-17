package com.collarmc.protocol.keepalive;

import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.security.ClientIdentity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class KeepAliveRequest extends ProtocolRequest {
    @JsonCreator
    public KeepAliveRequest(@JsonProperty("identity") ClientIdentity identity) {
        super(identity);
    }
}
