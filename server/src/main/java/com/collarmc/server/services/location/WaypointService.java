package com.collarmc.server.services.location;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.waypoints.EncryptedWaypoint;
import com.collarmc.protocol.waypoints.CreateWaypointRequest;
import com.collarmc.protocol.waypoints.GetWaypointsRequest;
import com.collarmc.protocol.waypoints.RemoveWaypointRequest;
import com.collarmc.server.services.profiles.storage.ProfileStorage;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages private waypoints
 */
public final class WaypointService {
    private static final String WAYPOINT_BLOB_TYPE = "W";

    private final ProfileStorage storage;

    public WaypointService(ProfileStorage storage) {
        this.storage = storage;
    }

    public void createWaypoint(ClientIdentity identity, CreateWaypointRequest req) {
        storage.store(identity.id(), req.waypointId, req.waypoint, WAYPOINT_BLOB_TYPE);
    }

    public void removeWaypoint(ClientIdentity identity, RemoveWaypointRequest req) {
        storage.delete(identity.id(), req.waypointId);
    }

    public List<EncryptedWaypoint> getWaypoints(ClientIdentity identity, GetWaypointsRequest req) {
        List<ProfileStorage.Blob> blobs = storage.find(identity.id(), WAYPOINT_BLOB_TYPE);
        return blobs.stream().map(blob -> new EncryptedWaypoint(blob.key, blob.data)).collect(Collectors.toList());
    }
}
