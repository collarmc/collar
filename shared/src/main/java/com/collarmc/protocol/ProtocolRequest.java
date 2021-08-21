package com.collarmc.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.collarmc.api.identity.ClientIdentity;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "t")
public abstract class ProtocolRequest {
    // TODO: replace
    @JsonProperty("identity")
    @Deprecated
    public final ClientIdentity identity;

    @JsonCreator
    public ProtocolRequest(@JsonProperty("identity") ClientIdentity identity) {
        this.identity = identity;
    }
}
