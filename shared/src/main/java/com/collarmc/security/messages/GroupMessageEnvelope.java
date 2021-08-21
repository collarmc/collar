package com.collarmc.security.messages;

import com.collarmc.api.identity.Identity;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message envelope for group messages
 */
public final class GroupMessageEnvelope {
    @JsonProperty("r")
    public final Identity recipient;
    @JsonProperty("m")
    public final byte[] message;

    public GroupMessageEnvelope(@JsonProperty("r") Identity recipient, @JsonProperty("m") byte[] message) {
        this.recipient = recipient;
        this.message = message;
    }
}
