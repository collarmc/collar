package com.collarmc.server.services.location;

import com.collarmc.api.groups.Group;
import com.collarmc.api.groups.Member;
import com.collarmc.api.groups.MemberSource;
import com.collarmc.api.groups.MembershipState;
import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.profiles.PublicProfile;
import com.collarmc.api.session.Player;
import com.collarmc.protocol.location.*;
import com.collarmc.security.messages.GroupMessageEnvelope;
import com.collarmc.server.Services;
import com.collarmc.server.protocol.BatchProtocolResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class PlayerLocationService {

    private static final Logger LOGGER = LogManager.getLogger(PlayerLocationService.class.getName());

    private final NearbyGroups nearbyGroups = new NearbyGroups();

    // Group to players
    private final ConcurrentMap<UUID, Set<UUID>> playersSharing = new ConcurrentHashMap<>();
    private final Services services;

    public PlayerLocationService(Services services) {
        this.services = services;
    }

    public void startSharing(ClientIdentity identity, StartSharingLocationRequest req) {
        services.sessions.findPlayer(identity).ifPresent(player -> {
            playersSharing.compute(req.groupId, (uuid, players) -> {
                if (players == null) {
                    players = new HashSet<>();
                }
                players.add(player.identity.id());
                return players;
            });
            LOGGER.info("Player " + player + " started sharing location with group " + req.groupId);
        });
    }

    public Optional<BatchProtocolResponse> stopSharing(ClientIdentity identity, StopSharingLocationRequest req) {
        return services.sessions.findPlayer(identity).flatMap(player -> stopSharing(req.groupId, player));
    }

    public Optional<BatchProtocolResponse> stopSharing(Player player) {
        return services.sessions.getIdentity(player)
                .map(identity -> {
                    BatchProtocolResponse responses = new BatchProtocolResponse();
                    List<BatchProtocolResponse> allResponses = playersSharing.entrySet().stream().filter(entry -> entry.getValue().contains(player.identity.id()))
                            .map(Map.Entry::getKey)
                            .map(uuid -> stopSharing(uuid, player))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(Collectors.toList());

                    for (BatchProtocolResponse response : allResponses) {
                        responses.concat(response);
                    }
                    return responses;
        });
    }

    /**
     * Updates other players with the senders location if they have sharing enabled for these groups
     * @param req of the location
     * @return {@link LocationUpdatedResponse} responses to send to clients
     */
    public Optional<BatchProtocolResponse> updateLocation(ClientIdentity identity, UpdateLocationRequest req) {
        return services.sessions.findPlayer(identity).flatMap(player -> createLocationResponses(player, new LocationUpdatedResponse(req.group, player, req.location)));
    }

    private Optional<BatchProtocolResponse> stopSharing(UUID groupId, Player player) {
        LOGGER.info("Player " + player + " stopped sharing location with group " + groupId);
        LocationUpdatedResponse locationUpdatedResponse = new LocationUpdatedResponse(groupId, player, null);
        Optional<BatchProtocolResponse> responses = createLocationResponses(player, locationUpdatedResponse);
        playersSharing.compute(groupId, (uuid, players) -> {
            if (players == null) {
                return null;
            }
            players.remove(player.identity.id());
            return players.isEmpty() ? null : players;
        });
        return responses;
    }

    public Optional<BatchProtocolResponse> updateNearbyGroups(ClientIdentity identity, UpdateNearbyRequest req) {
        Optional<Player> player = services.sessions.findPlayer(identity);
        if (player.isEmpty()) {
            return Optional.empty();
        }
        PublicProfile profile = services.profileCache.getById(player.get().identity.id()).orElseThrow(() -> new IllegalStateException("could not find profile " + player.get().identity.id())).toPublic();
        NearbyGroups.Result result = this.nearbyGroups.updateNearbyGroups(new MemberSource(player.get(), profile), req.nearbyHashes);
        return services.groups.updateNearbyGroups(result);
    }

    private Optional<BatchProtocolResponse> createLocationResponses(Player sender, LocationUpdatedResponse resp) {
        BatchProtocolResponse responses = new BatchProtocolResponse();
        // Find all the groups the requesting player is a member of
        Set<UUID> sharingWithGroups = playersSharing.entrySet().stream().filter(entry -> entry.getValue().contains(sender.identity.id()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        Set<Group> memberGroups = services.groups.findGroups(sharingWithGroups);
        // Keep track of players we have sent to, so we do not send them duplicate messages (e.g. if they share membership of 2 or more groups)
        HashSet<Player> uniquePlayers = new HashSet<>();
        GroupMessageEnvelope messageEnvelope = resp.location == null ? null : new GroupMessageEnvelope(resp.location);
        for (Group group : memberGroups) {
            for (Member member : group.members) {
                Player memberPlayer = member.player;
                // Do not send to self
                if (memberPlayer.equals(sender)) {
                    continue;
                }
                if (uniquePlayers.contains(memberPlayer) || member.membershipState != MembershipState.ACCEPTED) {
                    continue;
                }
                if (messageEnvelope != null && !messageEnvelope.messages.containsKey(memberPlayer.identity.id())) {
                    continue;
                }
                services.sessions.getIdentity(memberPlayer).ifPresent(identity -> {
                    uniquePlayers.add(memberPlayer);
                    byte[] location = messageEnvelope == null ? null : messageEnvelope.messages.get(memberPlayer.identity.id()).contents;
                    responses.add(identity, new LocationUpdatedResponse(resp.group, sender, location));
                });
            }
        }
        return responses.optional();
    }

    public void removePlayerState(Player player) {
        this.nearbyGroups.removePlayerState(player);
    }
}
