package com.collarmc.protocol.identity;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class CreateTrustRequest extends ProtocolRequest {
    @JsonProperty("id")
    public final Long id;
    @JsonProperty("recipient")
    public final ClientIdentity recipient;

    public CreateTrustRequest(@JsonProperty("id") Long id,
                              @JsonProperty("recipient") ClientIdentity recipient) {
        this.id = id;
        this.recipient = recipient;
    }
}
