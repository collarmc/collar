package com.collarmc.api.groups.http;

import com.collarmc.api.groups.Group;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class GetGroupsResponse {
    @JsonProperty("groups")
    public final List<Group> groups;

    public GetGroupsResponse(@JsonProperty("groups") List<Group> groups) {
        this.groups = groups;
    }
}
