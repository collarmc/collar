package com.collarmc.api.groups.http;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateGroupTokenResponse {
    @JsonProperty("token")
    public final String token;

    public CreateGroupTokenResponse(@JsonProperty("token") String token) {
        this.token = token;
    }
}
