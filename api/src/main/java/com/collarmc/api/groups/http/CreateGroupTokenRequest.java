package com.collarmc.api.groups.http;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class CreateGroupTokenRequest {
    @JsonProperty("group")
    public final UUID group;

    public CreateGroupTokenRequest(@JsonProperty("group") UUID group) {
        this.group = group;
    }
}
