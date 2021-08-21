package com.collarmc.protocol.identity;

import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.api.identity.ServerIdentity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.api.identity.ClientIdentity;

public final class CreateTrustResponse extends ProtocolResponse {
    @JsonProperty("id")
    public final Long id;
    @JsonProperty("sender")
    public final ClientIdentity sender;

    @JsonCreator
    public CreateTrustResponse(@JsonProperty("identity") ServerIdentity identity,
                               @JsonProperty("id") Long id,
                               @JsonProperty("sender") ClientIdentity sender) {
        super(identity);
        this.id = id;
        this.sender = sender;
    }
}
