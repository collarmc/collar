package com.collarmc.sdht.events;

import com.collarmc.sdht.Record;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.api.identity.ClientIdentity;

public final class CreateEntryEvent extends AbstractSDHTEvent {
    @JsonProperty("recipient")
    private final ClientIdentity recipient;
    @JsonProperty("record")
    public final Record record;
    @JsonProperty("content")
    public final byte[] content;

    public CreateEntryEvent(@JsonProperty("sender") ClientIdentity sender,
                            @JsonProperty("recipient") ClientIdentity recipient,
                            @JsonProperty("record") Record record,
                            @JsonProperty("content") byte[] content) {
        super(sender);
        this.recipient = recipient;
        this.record = record;
        this.content = content;
    }
}
