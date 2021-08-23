package com.collarmc.sdht.events;

import com.collarmc.api.identity.ClientIdentity;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "t")
public abstract class AbstractSDHTEvent {
    @JsonProperty("sender")
    public final ClientIdentity sender;

    public AbstractSDHTEvent(@JsonProperty("sender") ClientIdentity sender) {
        this.sender = sender;
    }
}
