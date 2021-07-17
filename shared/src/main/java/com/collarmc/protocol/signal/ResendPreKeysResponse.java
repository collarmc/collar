package com.collarmc.protocol.signal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.security.ServerIdentity;

/**
 * Used to get the client to re-exchange keys by sending a {@link SendPreKeysRequest} to the server
 */
public final class ResendPreKeysResponse extends ProtocolResponse {
    public ResendPreKeysResponse(@JsonProperty("identity") ServerIdentity identity) {
        super(identity);
    }
}
