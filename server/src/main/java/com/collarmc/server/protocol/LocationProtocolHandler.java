package com.collarmc.server.protocol;

import org.eclipse.jetty.websocket.api.Session;
import com.collarmc.api.session.Player;
import com.collarmc.api.waypoints.EncryptedWaypoint;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.protocol.location.StartSharingLocationRequest;
import com.collarmc.protocol.location.StopSharingLocationRequest;
import com.collarmc.protocol.location.UpdateLocationRequest;
import com.collarmc.protocol.location.UpdateNearbyRequest;
import com.collarmc.protocol.waypoints.CreateWaypointRequest;
import com.collarmc.protocol.waypoints.GetWaypointsRequest;
import com.collarmc.protocol.waypoints.GetWaypointsResponse;
import com.collarmc.protocol.waypoints.RemoveWaypointRequest;
import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.identity.ServerIdentity;
import com.collarmc.server.CollarServer;
import com.collarmc.server.services.location.PlayerLocationService;
import com.collarmc.server.services.location.WaypointService;

import java.util.List;
import java.util.function.BiConsumer;

public class LocationProtocolHandler extends ProtocolHandler {

    private final PlayerLocationService playerLocations;
    private final WaypointService waypoints;
    private final ServerIdentity serverIdentity;

    public LocationProtocolHandler(PlayerLocationService playerLocations, WaypointService waypoints, ServerIdentity serverIdentity) {
        this.playerLocations = playerLocations;
        this.waypoints = waypoints;
        this.serverIdentity = serverIdentity;
    }

    @Override
    public boolean handleRequest(CollarServer collar, ProtocolRequest req, BiConsumer<ClientIdentity, ProtocolResponse> sender) {
        if (req instanceof StartSharingLocationRequest) {
            StartSharingLocationRequest request = (StartSharingLocationRequest)req;
            playerLocations.startSharing(request);
            return true;
        } else if (req instanceof StopSharingLocationRequest) {
            StopSharingLocationRequest request = (StopSharingLocationRequest) req;
            playerLocations.stopSharing(request).ifPresent(response -> sender.accept(null, response));
            return true;
        } else if (req instanceof UpdateLocationRequest) {
            UpdateLocationRequest request = (UpdateLocationRequest) req;
            playerLocations.updateLocation(request).ifPresent(response -> sender.accept(request.identity, response));
            return true;
        } else if (req instanceof UpdateNearbyRequest) {
            UpdateNearbyRequest request = (UpdateNearbyRequest) req;
            playerLocations.updateNearbyGroups(request).ifPresent(response -> sender.accept(null, response));
            return true;
        } else if (req instanceof CreateWaypointRequest) {
            CreateWaypointRequest request = (CreateWaypointRequest) req;
            waypoints.createWaypoint(request);
            return true;
        } else if (req instanceof RemoveWaypointRequest) {
            RemoveWaypointRequest request = (RemoveWaypointRequest) req;
            waypoints.removeWaypoint(request);
            return true;
        } else if (req instanceof GetWaypointsRequest) {
            GetWaypointsRequest request = (GetWaypointsRequest) req;
            List<EncryptedWaypoint> waypoints = this.waypoints.getWaypoints(request);
            sender.accept(req.identity, new GetWaypointsResponse(serverIdentity, waypoints));
            return true;
        }
        return false;
    }

    @Override
    public void onSessionStopping(ClientIdentity identity, Player player, BiConsumer<Session, ProtocolResponse> sender) {
        playerLocations.stopSharing(player).ifPresent(response -> {
            sender.accept(null, response);
        });
        playerLocations.removePlayerState(player);
    }
}
