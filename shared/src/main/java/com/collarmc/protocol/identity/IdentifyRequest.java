package com.collarmc.protocol.identity;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.hash.Hashing;

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
        if (token != null) {
            System.out.println(Hashing.sha256().hashBytes(identity.publicKey.key).toString());
            System.err.println(Hashing.sha256().hashBytes(token).toString());
        }
    }

    /**
     * @return identify request that will prompt the client to negotiate an identity with collar
     */
    public static IdentifyRequest unknown() {
        return new IdentifyRequest(null, null);
    }
}
