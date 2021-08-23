package com.collarmc.protocol.groups;

import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class TransferGroupOwnershipRequest extends ProtocolRequest {
    @JsonProperty("group")
    public final UUID group;
    @JsonProperty("profile")
    public final UUID profile;

    public TransferGroupOwnershipRequest(@JsonProperty("group") UUID group,
                                         @JsonProperty("profile") UUID profile) {
        this.group = group;
        this.profile = profile;
    }
}
