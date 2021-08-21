package com.collarmc.protocol.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.api.identity.ClientIdentity;

import java.util.UUID;

/**
 * Sent on response to {@link JoinGroupResponse} to distribute all keys
 */
public final class AcknowledgedGroupJoinedRequest extends ProtocolRequest {
    @JsonProperty("recipient")
    public final ClientIdentity recipient;

    @JsonProperty("group")
    public final UUID group;

    public AcknowledgedGroupJoinedRequest(@JsonProperty("identity") ClientIdentity identity,
                                          @JsonProperty("recipient") ClientIdentity recipient,
                                          @JsonProperty("group") UUID group) {
        super(identity);
        this.recipient = recipient;
        this.group = group;
    }
}
