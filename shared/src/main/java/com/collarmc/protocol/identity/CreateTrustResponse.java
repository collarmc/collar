package com.collarmc.protocol.identity;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.protocol.ProtocolResponse;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class CreateTrustResponse extends ProtocolResponse {
    @JsonProperty("id")
    public final Long id;
    @JsonProperty("sender")
    public final ClientIdentity sender;

    @JsonCreator
    public CreateTrustResponse(@JsonProperty("id") Long id,
                               @JsonProperty("sender") ClientIdentity sender) {
        this.id = id;
        this.sender = sender;
    }
}
