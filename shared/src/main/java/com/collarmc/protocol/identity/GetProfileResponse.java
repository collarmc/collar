package com.collarmc.protocol.identity;

import com.collarmc.api.profiles.PublicProfile;
import com.collarmc.protocol.ProtocolResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class GetProfileResponse extends ProtocolResponse {
    @JsonProperty("id")
    public final UUID id;
    @JsonProperty("profile")
    public final PublicProfile profile;

    public GetProfileResponse(@JsonProperty("id") UUID id,
                              @JsonProperty("profile") PublicProfile profile) {
        this.id = id;
        this.profile = profile;
    }
}
