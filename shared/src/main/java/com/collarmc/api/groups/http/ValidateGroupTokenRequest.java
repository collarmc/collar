package com.collarmc.api.groups.http;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class ValidateGroupTokenRequest {
    @JsonProperty("token")
    public final String token;
    @JsonProperty("group")
    public final String group;

    public ValidateGroupTokenRequest(@JsonProperty("token") String token, @JsonProperty("group") String group) {
        this.token = token;
        this.group = group;
    }
}
