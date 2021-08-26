package com.collarmc.protocol.groups;

import com.collarmc.api.groups.Group;
import com.collarmc.protocol.ProtocolResponse;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Sent by the server when it has issued a new Group
 * Sent to the sender of {@link CreateGroupRequest}
 */
public final class CreateGroupResponse extends ProtocolResponse {
    @JsonProperty("group")
    public final Group group;

    @JsonCreator
    public CreateGroupResponse(@JsonProperty("group") Group group) {
        this.group = group;
    }
}
