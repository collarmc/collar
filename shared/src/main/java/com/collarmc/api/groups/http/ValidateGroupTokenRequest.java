package com.collarmc.api.groups.http;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ValidateGroupTokenRequest {
    @JsonProperty("token")
    public final String token;

    public ValidateGroupTokenRequest(String token) {
        this.token = token;
    }
}
