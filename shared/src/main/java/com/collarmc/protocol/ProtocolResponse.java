package com.collarmc.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.collarmc.api.identity.ServerIdentity;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "t")
public abstract class ProtocolResponse {
    @JsonProperty("identity")
    public final ServerIdentity identity;

    @JsonCreator
    public ProtocolResponse(@JsonProperty("identity") ServerIdentity identity) {
        this.identity = identity;
    }
}
