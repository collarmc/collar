package com.collarmc.protocol.identity;

import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.api.identity.ClientIdentity;

public final class CreateTrustRequest extends ProtocolRequest {
    @JsonProperty("id")
    public final Long id;
    @JsonProperty("recipient")
    public final ClientIdentity recipient;

    public CreateTrustRequest(@JsonProperty("identity") ClientIdentity identity,
                              @JsonProperty("id") Long id,
                              @JsonProperty("recipient") ClientIdentity recipient) {
        super(identity);
        this.id = id;
        this.recipient = recipient;
    }
}
