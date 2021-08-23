package com.collarmc.protocol.identity;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.protocol.ProtocolResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    public GetIdentityResponse(@JsonProperty("id") Long id,
                               @JsonProperty("found") ClientIdentity found,
                               @JsonProperty("player") UUID player) {
        this.id = id;
        this.found = found;
        this.player = player;
    }
}
