package com.collarmc.protocol.identity;

import com.collarmc.api.identity.ServerIdentity;
import com.collarmc.api.profiles.PublicProfile;
import com.collarmc.protocol.ProtocolResponse;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class IdentifyResponse extends ProtocolResponse {

    @JsonProperty("identity")
    public final ServerIdentity identity;

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

    @JsonProperty("apiToken")
    public final String apiToken;

    /**
     * Token encrypted
     */
    @JsonProperty("token")
    public final byte[] token;

    @JsonCreator
    public IdentifyResponse(@JsonProperty("identity") ServerIdentity identity,
                            @JsonProperty("profile") PublicProfile profile,
                            @JsonProperty("minecraftServerId") String minecraftServerId,
                            @JsonProperty("sharedSecret") byte[] minecraftSharedSecret,
                            @JsonProperty("apiToken") String apiToken,
                            @JsonProperty("token") byte[] token) {
        this.identity = identity;
        this.profile = profile;
        this.minecraftServerId = minecraftServerId;
        this.minecraftSharedSecret = minecraftSharedSecret;
        this.apiToken = apiToken;
        this.token = token;
    }
}
