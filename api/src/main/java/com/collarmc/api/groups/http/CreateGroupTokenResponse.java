package com.collarmc.api.groups.http;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class CreateGroupTokenResponse {
    @JsonProperty("token")
    public final byte[] token;

    public CreateGroupTokenResponse(@JsonProperty("token") byte[] token) {
        this.token = token;
    }
}
