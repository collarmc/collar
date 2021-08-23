package com.collarmc.protocol.devices;

import com.collarmc.protocol.ProtocolResponse;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class RegisterDeviceResponse extends ProtocolResponse {
    @JsonProperty("approvalUrl")
    public final String approvalUrl;
    @JsonProperty("approvalToken")
    public final String approvalToken;

    @JsonCreator
    public RegisterDeviceResponse(@JsonProperty("approvalUrl") String approvalUrl,
                                  @JsonProperty("approvalToken") String approvalToken) {
        this.approvalUrl = approvalUrl;
        this.approvalToken = approvalToken;
    }
}
