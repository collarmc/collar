package com.collarmc.protocol.trust;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.security.ClientIdentity;

public final class CheckTrustRelationshipRequest extends ProtocolRequest {
    @JsonCreator
    public CheckTrustRelationshipRequest(@JsonProperty("identity") ClientIdentity identity) {
        super(identity);
    }
}
