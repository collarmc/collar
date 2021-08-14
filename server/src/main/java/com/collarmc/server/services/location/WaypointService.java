package com.collarmc.server.services.location;

import com.collarmc.api.waypoints.EncryptedWaypoint;
import com.collarmc.server.services.profiles.storage.ProfileStorage;
import com.collarmc.protocol.waypoints.CreateWaypointRequest;
import com.collarmc.protocol.waypoints.GetWaypointsRequest;
import com.collarmc.protocol.waypoints.RemoveWaypointRequest;

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

    public void createWaypoint(CreateWaypointRequest req) {
        storage.store(req.identity.profile, req.waypointId, req.waypoint, WAYPOINT_BLOB_TYPE);
    }

    public void removeWaypoint(RemoveWaypointRequest req) {
        storage.delete(req.identity.profile, req.waypointId);
    }

    public List<EncryptedWaypoint> getWaypoints(GetWaypointsRequest req) {
        List<ProfileStorage.Blob> blobs = storage.find(req.identity.profile, WAYPOINT_BLOB_TYPE);
        return blobs.stream().map(blob -> new EncryptedWaypoint(blob.key, blob.data)).collect(Collectors.toList());
    }
}
