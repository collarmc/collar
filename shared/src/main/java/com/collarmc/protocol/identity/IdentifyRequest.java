package com.collarmc.protocol.identity;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * When sent with a null identity, prompts a login response
 */
public final class IdentifyRequest extends ProtocolRequest {

    @JsonProperty("identity")
    public final ClientIdentity identity;

    @JsonProperty("token")
    public final byte[] token;

    @JsonCreator
    public IdentifyRequest(@JsonProperty("identity") ClientIdentity identity,
                           @JsonProperty("token") byte[] token) {
        this.identity = identity;
        this.token = token;
    }

    /**
     * @return identify request that will prompt the client to negotiate an identity with collar
     */
    public static IdentifyRequest unknown() {
        return new IdentifyRequest(null, null);
    }
}
