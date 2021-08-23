package com.collarmc.protocol.location;

import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Sent by the client to update the players current location
 * There is no response sent back to the sender for this request as this would be too noisy.
 */
public final class UpdateLocationRequest extends ProtocolRequest {
    /** Group to share location with **/
    @JsonProperty("group")
    public final UUID group;
    /** Location **/
    @JsonProperty("location")
    public final byte[] location;

    @JsonCreator
    public UpdateLocationRequest(@JsonProperty("group") UUID group,
                                 @JsonProperty("location") byte[] location) {
        this.group = group;
        this.location = location;
    }
}
