package com.collarmc.protocol.identity;

import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class GetProfileRequest extends ProtocolRequest {
    @JsonProperty("profile")
    public final UUID profile;

    public GetProfileRequest(@JsonProperty("profile") UUID profile) {
        this.profile = profile;
    }
}
