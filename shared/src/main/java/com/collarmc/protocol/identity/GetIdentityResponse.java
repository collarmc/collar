package com.collarmc.protocol.identity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.security.ClientIdentity;
import com.collarmc.security.ServerIdentity;

import java.util.UUID;

public final class GetIdentityResponse extends ProtocolResponse {

    /**
     * Request identifier
     */
    public final Long id;

    /**
     * The player that was found
     */
    public final ClientIdentity found;

    @JsonProperty("player")
    public final UUID player;

    public GetIdentityResponse(@JsonProperty("identity") ServerIdentity identity,
                               @JsonProperty("id") Long id,
                               @JsonProperty("found") ClientIdentity found,
                               @JsonProperty("player") UUID player) {
        super(identity);
        this.id = id;
        this.found = found;
        this.player = player;
    }
}
