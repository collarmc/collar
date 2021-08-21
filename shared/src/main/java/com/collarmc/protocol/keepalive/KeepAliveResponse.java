package com.collarmc.protocol.keepalive;

import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.api.identity.ServerIdentity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class KeepAliveResponse extends ProtocolResponse {
    @JsonCreator
    public KeepAliveResponse(@JsonProperty("identity") ServerIdentity identity) {
        super(identity);
    }
}
