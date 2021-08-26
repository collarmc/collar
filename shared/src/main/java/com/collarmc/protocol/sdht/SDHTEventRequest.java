package com.collarmc.protocol.sdht;

import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.sdht.events.AbstractSDHTEvent;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class SDHTEventRequest extends ProtocolRequest {
    @JsonProperty("event")
    public final AbstractSDHTEvent event;

    public SDHTEventRequest(@JsonProperty("event") AbstractSDHTEvent event) {
        this.event = event;
    }
}
