package com.collarmc.protocol.groups;

import com.collarmc.api.groups.Group;
import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.session.Player;
import com.collarmc.protocol.ProtocolResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Sent back to you when your keys are distributed and the Group is ready to use
 */
public final class AcknowledgedGroupJoinedResponse extends ProtocolResponse {
    @JsonProperty("sender")
    public final ClientIdentity sender;

    @JsonProperty("player")
    public final Player player;

    @JsonProperty("group")
    public final Group group;

    public AcknowledgedGroupJoinedResponse(@JsonProperty("sender") ClientIdentity sender,
                                           @JsonProperty("player") Player player,
                                           @JsonProperty("group") Group group) {
        this.sender = sender;
        this.player = player;
        this.group = group;
    }
}
