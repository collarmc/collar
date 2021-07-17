package com.collarmc.protocol.signal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.security.ClientIdentity;

public final class SendPreKeysRequest extends ProtocolRequest {
    @JsonProperty("preKeyBundle")
    public final byte[] preKeyBundle;

    @JsonCreator
    public SendPreKeysRequest(@JsonProperty("identity") ClientIdentity identity,
                              @JsonProperty("preKeyBundle") byte[] preKeyBundle) {
        super(identity);
        this.preKeyBundle = preKeyBundle;
    }
}
