package com.collarmc.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.collarmc.security.ClientIdentity;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "t")
public abstract class ProtocolRequest {
    @JsonProperty("identity")
    public final ClientIdentity identity;

    @JsonCreator
    public ProtocolRequest(@JsonProperty("identity") ClientIdentity identity) {
        this.identity = identity;
    }
}
