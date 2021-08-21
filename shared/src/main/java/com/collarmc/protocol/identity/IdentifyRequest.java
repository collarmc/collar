package com.collarmc.protocol.identity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.api.identity.ClientIdentity;

/**
 * When sent with a null identity, prompts a login response
 */
public final class IdentifyRequest extends ProtocolRequest {
    @JsonProperty("token")
    public final byte[] token;

    @JsonCreator
    public IdentifyRequest(@JsonProperty("identity") ClientIdentity identity,
                           @JsonProperty("token") byte[] token) {
        super(identity);
        this.token = token;
    }

    /**
     * @return identify request that will prompt the client to negotiate an identity with collar
     */
    public static IdentifyRequest unknown() {
        return new IdentifyRequest(null, null);
    }
}
