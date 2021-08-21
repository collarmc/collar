package com.collarmc.protocol.groups;

import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.api.identity.ServerIdentity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.api.groups.Group;

/**
 * Sent by the server when it has issued a new Group
 * Sent to the sender of {@link CreateGroupRequest}
 */
public final class CreateGroupResponse extends ProtocolResponse {
    @JsonProperty("group")
    public final Group group;

    @JsonCreator
    public CreateGroupResponse(@JsonProperty("identity") ServerIdentity identity,
                               @JsonProperty("group") Group group) {
        super(identity);
        this.group = group;
    }
}
