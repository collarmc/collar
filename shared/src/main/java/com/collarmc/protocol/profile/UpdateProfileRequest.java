package com.collarmc.protocol.profile;

import com.collarmc.api.waypoints.EncryptedWaypoint;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.api.identity.ClientIdentity;

public class UpdateProfileRequest extends ProtocolRequest {
    public final EncryptedWaypoint addWaypoint;

    public UpdateProfileRequest(ClientIdentity identity, EncryptedWaypoint addWaypoint) {
        super(identity);
        this.addWaypoint = addWaypoint;
    }
}
