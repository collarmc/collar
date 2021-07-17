package com.collarmc.protocol.identity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.security.ClientIdentity;
import com.collarmc.security.ServerIdentity;

public final class GetIdentityResponse extends ProtocolResponse {

    /**
     * Request identifier
     */
    public final Long id;

    /**
     * The player that was found
     */
    public final ClientIdentity found;

    public GetIdentityResponse(@JsonProperty("identity") ServerIdentity identity,
                               @JsonProperty("id") Long id,
                               @JsonProperty("found") ClientIdentity found) {
        super(identity);
        this.id = id;
        this.found = found;
    }
}
