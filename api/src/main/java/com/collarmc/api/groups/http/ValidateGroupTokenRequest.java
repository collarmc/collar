package com.collarmc.api.groups.http;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class ValidateGroupTokenRequest {
    @JsonProperty("token")
    public final byte[] token;
    @JsonProperty("group")
    public final UUID group;

    public ValidateGroupTokenRequest(@JsonProperty("token") byte[] token, @JsonProperty("group") UUID group) {
        this.token = token;
        this.group = group;
    }
}
