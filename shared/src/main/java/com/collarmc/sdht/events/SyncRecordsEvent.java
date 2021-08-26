package com.collarmc.sdht.events;

import com.collarmc.api.identity.ClientIdentity;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Sent to all nodes by a joining node
 */
public final class SyncRecordsEvent extends AbstractSDHTEvent {
    @JsonProperty("namespace")
    public final UUID namespace;

    public SyncRecordsEvent(@JsonProperty("sender") ClientIdentity sender,
                            @JsonProperty("namespace") UUID namespace) {
        super(sender);
        this.namespace = namespace;
    }
}
