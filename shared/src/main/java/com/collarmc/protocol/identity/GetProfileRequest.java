package com.collarmc.protocol.identity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.api.identity.ClientIdentity;

import java.util.UUID;

public final class GetProfileRequest extends ProtocolRequest {
    @JsonProperty("profile")
    public final UUID profile;

    public GetProfileRequest(@JsonProperty("identify") ClientIdentity identity,
                             @JsonProperty("profile") UUID profile) {
        super(identity);
        this.profile = profile;
    }
}
