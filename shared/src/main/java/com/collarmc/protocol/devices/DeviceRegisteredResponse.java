package com.collarmc.protocol.devices;

import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.security.ServerIdentity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.api.profiles.PublicProfile;

public final class DeviceRegisteredResponse extends ProtocolResponse {
    @JsonProperty("profile")
    public final PublicProfile profile;
    @JsonProperty("deviceId")
    public final Integer deviceId;

    @JsonCreator
    public DeviceRegisteredResponse(@JsonProperty("identity") ServerIdentity identity, @JsonProperty("profile") PublicProfile profile, @JsonProperty("deviceId") Integer deviceId) {
        super(identity);
        this.profile = profile;
        this.deviceId = deviceId;
    }
}
