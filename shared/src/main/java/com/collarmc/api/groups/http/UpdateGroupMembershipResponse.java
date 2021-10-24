package com.collarmc.api.groups.http;

import com.collarmc.api.groups.Group;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class UpdateGroupMembershipResponse {
    @JsonProperty("group")
    public final Group group;

    public UpdateGroupMembershipResponse(@JsonProperty("group") Group group) {
        this.group = group;
    }
}
