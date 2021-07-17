package com.collarmc.protocol.location;

import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.security.ClientIdentity;

import java.util.Set;

public final class UpdateNearbyRequest extends ProtocolRequest {
    /**
     * sha256 hashes of player identity ids that the sender can see
     */
    public final Set<String> nearbyHashes;

    public UpdateNearbyRequest(@JsonProperty("identity") ClientIdentity identity,
                               @JsonProperty("nearbyHashes") Set<String> nearbyHashes) {
        super(identity);
        this.nearbyHashes = nearbyHashes;
    }
}
