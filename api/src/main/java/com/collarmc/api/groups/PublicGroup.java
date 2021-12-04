package com.collarmc.api.groups;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class PublicGroup {
    @JsonProperty("id")
    public final UUID id;
    @JsonProperty("name")
    public final String name;
    @JsonProperty("type")
    public final GroupType type;

    public PublicGroup(@JsonProperty("id") UUID id,
                       @JsonProperty("name") String name,
                       @JsonProperty("type") GroupType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }
}
