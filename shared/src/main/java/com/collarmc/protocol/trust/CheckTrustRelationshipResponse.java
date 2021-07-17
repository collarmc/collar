package com.collarmc.protocol.trust;

import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.security.ServerIdentity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class CheckTrustRelationshipResponse extends ProtocolResponse {
    @JsonCreator
    public CheckTrustRelationshipResponse(@JsonProperty("identity") ServerIdentity identity) {
        super(identity);
    }

    public static final class IsTrustedRelationshipResponse extends CheckTrustRelationshipResponse {
        @JsonCreator
        public IsTrustedRelationshipResponse(@JsonProperty("identity") ServerIdentity identity) {
            super(identity);
        }
    }

    public static final class IsUntrustedRelationshipResponse extends CheckTrustRelationshipResponse {
        @JsonCreator
        public IsUntrustedRelationshipResponse(@JsonProperty("identity") ServerIdentity identity) {
            super(identity);
        }
    }
}
