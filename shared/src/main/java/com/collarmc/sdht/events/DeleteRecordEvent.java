package com.collarmc.sdht.events;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.sdht.Record;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class DeleteRecordEvent extends AbstractSDHTEvent {
    @JsonProperty("delete")
    public final Record delete;

    public DeleteRecordEvent(@JsonProperty("sender") ClientIdentity sender,
                             @JsonProperty("delete") Record delete) {
        super(sender);
        this.delete = delete;
    }
}
