package com.collarmc.protocol.session;

import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.api.identity.ServerIdentity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class StartSessionResponse extends ProtocolResponse {
    @JsonCreator
    public StartSessionResponse(@JsonProperty("identity") ServerIdentity identity) {
        super(identity);
    }
}
