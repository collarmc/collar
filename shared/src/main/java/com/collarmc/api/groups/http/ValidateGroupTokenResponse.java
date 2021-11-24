package com.collarmc.api.groups.http;

import com.collarmc.api.groups.PublicGroup;
import com.collarmc.api.profiles.PublicProfile;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ValidateGroupTokenResponse {
    @JsonProperty("group")
    public final PublicGroup group;
    @JsonProperty("profile")
    public final PublicProfile profile;

    public ValidateGroupTokenResponse(
            @JsonProperty("group") PublicGroup group,
            @JsonProperty("profile") PublicProfile profile) {
        this.group = group;
        this.profile = profile;
    }
}
