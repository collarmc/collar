package com.collarmc.security.discrete;

import com.collarmc.security.Identity;
import com.fasterxml.jackson.annotation.JsonProperty;

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
