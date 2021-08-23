package com.collarmc.protocol.groups;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Sent on response to {@link JoinGroupResponse} to distribute all keys
 */
public final class AcknowledgedGroupJoinedRequest extends ProtocolRequest {
    @JsonProperty("recipient")
    public final ClientIdentity recipient;

    @JsonProperty("group")
    public final UUID group;

    public AcknowledgedGroupJoinedRequest(@JsonProperty("recipient") ClientIdentity recipient,
                                          @JsonProperty("group") UUID group) {
        this.recipient = recipient;
        this.group = group;
    }
}
