package com.collarmc.api.groups.http;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateGroupMembershipTokenResponse {
    @JsonProperty("token")
    public final String token;

    public CreateGroupMembershipTokenResponse(@JsonProperty("token") String token) {
        this.token = token;
    }
}
