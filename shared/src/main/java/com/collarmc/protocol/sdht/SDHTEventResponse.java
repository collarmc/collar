package com.collarmc.protocol.sdht;

import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.sdht.events.AbstractSDHTEvent;
import com.collarmc.api.identity.ServerIdentity;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class SDHTEventResponse extends ProtocolResponse {
    @JsonProperty("event")
    public final AbstractSDHTEvent event;

    public SDHTEventResponse(@JsonProperty("identity") ServerIdentity identity,
                             @JsonProperty("event") AbstractSDHTEvent event) {
        super(identity);
        this.event = event;
    }
}
