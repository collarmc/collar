package com.collarmc.protocol.identity;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Used to lookup a {@link ClientIdentity}
 */
public final class GetIdentityRequest extends ProtocolRequest {
    /**
     * Request identifier
     */
    public final Long id;

    /**
     * Player id to map to an identity
     */
    public final UUID player;

    public GetIdentityRequest(@JsonProperty("id") Long id,
                              @JsonProperty("player") UUID player) {
        this.id = id;
        this.player = player;
    }
}
