package com.collarmc.protocol.sdht;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.sdht.events.AbstractSDHTEvent;
import com.collarmc.api.identity.ClientIdentity;

public final class SDHTEventRequest extends ProtocolRequest {
    @JsonProperty("event")
    public final AbstractSDHTEvent event;

    public SDHTEventRequest(@JsonProperty("identity") ClientIdentity identity,
                            @JsonProperty("event") AbstractSDHTEvent event) {
        super(identity);
        this.event = event;
    }
}
