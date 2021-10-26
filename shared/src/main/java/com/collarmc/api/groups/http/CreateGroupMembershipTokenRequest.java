package com.collarmc.api.groups.http;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class CreateGroupMembershipTokenRequest {
    @JsonProperty("group")
    public final UUID group;

    public CreateGroupMembershipTokenRequest(@JsonProperty("group") UUID group) {
        this.group = group;
    }
}
