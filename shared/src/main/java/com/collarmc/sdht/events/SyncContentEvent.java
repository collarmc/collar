package com.collarmc.sdht.events;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.sdht.Record;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class SyncContentEvent extends AbstractSDHTEvent {
    @JsonProperty("recipient")
    public final ClientIdentity recipient;
    @JsonProperty("record")
    public final Record record;

    public SyncContentEvent(@JsonProperty("sender") ClientIdentity sender,
                            @JsonProperty("recipient") ClientIdentity recipient,
                            @JsonProperty("record") Record record) {
        super(sender);
        this.recipient = recipient;
        this.record = record;
    }
}
