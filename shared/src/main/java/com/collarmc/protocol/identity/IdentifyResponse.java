package com.collarmc.protocol.identity;

import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.api.identity.ServerIdentity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.api.profiles.PublicProfile;

public final class IdentifyResponse extends ProtocolResponse {
    @JsonProperty("profile")
    public final PublicProfile profile;

    /**
     * The Collar servers emulated Mojang server id
     */
    @JsonProperty("minecraftServerId")
    public final String minecraftServerId;

    /**
     * The collar servers emulated Mojang shared secret
     */
    @JsonProperty("minecraftSharedSecret")
    public final byte[] minecraftSharedSecret;

    @JsonCreator
    public IdentifyResponse(@JsonProperty("identity") ServerIdentity identity,
                            @JsonProperty("profile") PublicProfile profile,
                            @JsonProperty("minecraftServerId") String minecraftServerId,
                            @JsonProperty("sharedSecret") byte[] minecraftSharedSecret) {
        super(identity);
        this.profile = profile;
        this.minecraftServerId = minecraftServerId;
        this.minecraftSharedSecret = minecraftSharedSecret;
    }
}
