package com.collarmc.api.groups.http;

import com.collarmc.api.groups.Group;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class AddGroupMemberResponse {
    @JsonProperty("group")
    private final Group group;

    public AddGroupMemberResponse(@JsonProperty("group") Group group) {
        this.group = group;
    }
}
