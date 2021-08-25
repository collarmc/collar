package com.collarmc.server.protocol;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.identity.ServerIdentity;
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
import com.collarmc.server.CollarServer;
import com.collarmc.server.Services;
import com.collarmc.server.services.location.PlayerLocationService;
import com.collarmc.server.services.location.WaypointService;
import org.eclipse.jetty.websocket.api.Session;

import java.util.List;
import java.util.function.BiConsumer;

public class LocationProtocolHandler extends ProtocolHandler {

    public LocationProtocolHandler(Services services) {
        super(services);
    }

    @Override
    public boolean handleRequest(CollarServer collar, ClientIdentity identity, ProtocolRequest req, BiConsumer<ClientIdentity, ProtocolResponse> sender) {
        if (req instanceof StartSharingLocationRequest) {
            StartSharingLocationRequest request = (StartSharingLocationRequest)req;
            services.playerLocations.startSharing(identity, request);
            return true;
        } else if (req instanceof StopSharingLocationRequest) {
            StopSharingLocationRequest request = (StopSharingLocationRequest) req;
            services.playerLocations.stopSharing(identity, request).ifPresent(response -> sender.accept(null, response));
            return true;
        } else if (req instanceof UpdateLocationRequest) {
            UpdateLocationRequest request = (UpdateLocationRequest) req;
            services.playerLocations.updateLocation(identity, request).ifPresent(response -> sender.accept(identity, response));
            return true;
        } else if (req instanceof UpdateNearbyRequest) {
            UpdateNearbyRequest request = (UpdateNearbyRequest) req;
            services.playerLocations.updateNearbyGroups(identity, request).ifPresent(response -> sender.accept(null, response));
            return true;
        } else if (req instanceof CreateWaypointRequest) {
            CreateWaypointRequest request = (CreateWaypointRequest) req;
            services.waypoints.createWaypoint(identity, request);
            return true;
        } else if (req instanceof RemoveWaypointRequest) {
            RemoveWaypointRequest request = (RemoveWaypointRequest) req;
            services.waypoints.removeWaypoint(identity, request);
            return true;
        } else if (req instanceof GetWaypointsRequest) {
            GetWaypointsRequest request = (GetWaypointsRequest) req;
            List<EncryptedWaypoint> waypoints = services.waypoints.getWaypoints(identity, request);
            sender.accept(identity, new GetWaypointsResponse(waypoints));
            return true;
        }
        return false;
    }

    @Override
    public void onSessionStopping(ClientIdentity identity, Player player, BiConsumer<Session, ProtocolResponse> sender) {
        services.playerLocations.stopSharing(player).ifPresent(response -> {
            sender.accept(null, response);
        });
        services.playerLocations.removePlayerState(player);
    }
}
