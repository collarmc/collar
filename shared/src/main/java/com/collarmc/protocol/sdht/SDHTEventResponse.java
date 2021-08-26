package com.collarmc.protocol.sdht;

import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.sdht.events.AbstractSDHTEvent;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class SDHTEventResponse extends ProtocolResponse {
    @JsonProperty("event")
    public final AbstractSDHTEvent event;

    public SDHTEventResponse(@JsonProperty("event") AbstractSDHTEvent event) {
        this.event = event;
    }
}
