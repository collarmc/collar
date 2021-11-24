package com.collarmc.api.groups.http;

import com.collarmc.api.groups.MembershipRole;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class UpdateGroupMembershipRequest {
    @JsonProperty("group")
    public final UUID group;
    @JsonProperty("profile")
    public final UUID profile;
    @JsonProperty("role")
    public final MembershipRole role;

    public UpdateGroupMembershipRequest(
            @JsonProperty("group") UUID group,
            @JsonProperty("profile") UUID profile,
            @JsonProperty("role") MembershipRole role) {
        this.group = group;
        this.profile = profile;
        this.role = role;
    }
}
