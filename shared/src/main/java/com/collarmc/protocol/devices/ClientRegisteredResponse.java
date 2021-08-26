package com.collarmc.protocol.devices;

import com.collarmc.api.identity.ServerIdentity;
import com.collarmc.api.profiles.PublicProfile;
import com.collarmc.protocol.ProtocolResponse;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ClientRegisteredResponse extends ProtocolResponse {
    @JsonProperty("serverIdentity")
    public final ServerIdentity serverIdentity;
    @JsonProperty("profile")
    public final PublicProfile profile;

    @JsonCreator
    public ClientRegisteredResponse(@JsonProperty("serverIdentity") ServerIdentity serverIdentity,
                                    @JsonProperty("profile") PublicProfile profile) {
        this.serverIdentity = serverIdentity;
        this.profile = profile;
    }
}
