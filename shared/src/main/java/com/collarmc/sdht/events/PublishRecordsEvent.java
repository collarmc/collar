package com.collarmc.sdht.events;

import com.collarmc.sdht.Record;
import com.collarmc.security.ClientIdentity;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public final class PublishRecordsEvent extends AbstractSDHTEvent {
    @JsonProperty("records")
    public final Set<Record> records;
    @JsonProperty("recipient")
    public final ClientIdentity recipient;

    public PublishRecordsEvent(@JsonProperty("sender") ClientIdentity sender,
                               @JsonProperty("records") Set<Record> records,
                               @JsonProperty("recipient") ClientIdentity recipient) {
        super(sender);
        this.records = records;
        this.recipient = recipient;
    }
}
