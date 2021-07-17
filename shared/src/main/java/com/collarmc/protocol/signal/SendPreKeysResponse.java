package com.collarmc.protocol.signal;

import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.security.ServerIdentity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class SendPreKeysResponse extends ProtocolResponse {
    @JsonProperty("preKeyBundle")
    public final byte[] preKeyBundle;

    @JsonCreator
    public SendPreKeysResponse(@JsonProperty("identity") ServerIdentity identity,
                               @JsonProperty("preKeyBundle") byte[] preKeyBundle) {
        super(identity);
        this.preKeyBundle = preKeyBundle;
    }
}
