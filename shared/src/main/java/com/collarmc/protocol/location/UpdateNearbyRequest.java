package com.collarmc.protocol.location;

import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public final class UpdateNearbyRequest extends ProtocolRequest {
    /**
     * sha256 hashes of player identity ids that the sender can see
     */
    public final Set<String> nearbyHashes;

    public UpdateNearbyRequest(@JsonProperty("nearbyHashes") Set<String> nearbyHashes) {
        this.nearbyHashes = nearbyHashes;
    }
}
