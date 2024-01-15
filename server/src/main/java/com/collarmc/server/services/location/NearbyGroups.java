package com.collarmc.server.services.location;

import com.collarmc.api.groups.Group;
import com.collarmc.api.groups.MemberSource;
import com.collarmc.api.session.Player;
import com.google.common.collect.Sets;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * State machine for managing dynamically created {@link Group}'s based on hashing
 * every players player entity list and comparing them.
 */
public final class NearbyGroups {

    private static final Logger LOGGER = LogManager.getLogger(NearbyGroups.class.getName());

    private final ConcurrentMap<MemberSource, Set<String>> playerHashes = new ConcurrentHashMap<>();
    private final ConcurrentMap<NearbyGroup, UUID> nearbyGroups = new ConcurrentHashMap<>();
    private final ConcurrentMap<MemberSource, Set<NearbyGroup>> playerToGroups = new ConcurrentHashMap<>();

    /**
     * Calculates any nearby groups for the minecraft player and anyone in the calculated group
     * Both parties must have entity hashes in common to form one or more groups with other players
     * @param source to create state for
     * @param hashes of players nearby
     * @return result delta
     */
    public Result updateNearbyGroups(MemberSource source, Set<String> hashes) {
        playerHashes.compute(source, (thePlayer, strings) -> hashes);
        Map<UUID, NearbyGroup> add = new HashMap<>();
        Map<UUID, NearbyGroup> remove = new HashMap<>();
        playerHashes.keySet().stream()
                .filter(anotherSource ->
                        anotherSource.player.minecraftPlayer != null &&
                                source.player.minecraftPlayer != null &&
                                anotherSource.player.minecraftPlayer.inServerWith(source.player.minecraftPlayer)
                                && !anotherSource.equals(source)
                ).forEach(anotherPlayer -> {
                    Set<String> otherPlayersHashes = playerHashes.get(anotherPlayer);
                    NearbyGroup group = new NearbyGroup(Set.of(source, anotherPlayer));
                    if (Sets.difference(hashes, otherPlayersHashes).isEmpty()) {
                        nearbyGroups.compute(group, (nearbyGroup, uuid) -> {
                            if (uuid == null) {
                                uuid = UUID.randomUUID();
                                add.put(uuid, group);
                            }
                            return uuid;
                        });
                        playerToGroups.compute(source, (thePlayer, playersGroups) -> {
                            playersGroups = playersGroups == null ? new HashSet<>() : playersGroups;
                            playersGroups.add(group);
                            return playersGroups;
                        });
                    } else {
                        UUID groupId = nearbyGroups.get(group);
                        nearbyGroups.remove(group);
                        if (groupId != null) {
                            remove.put(groupId, group);
                            playerToGroups.compute(source, (thePlayer, playersGroups) -> {
                                playersGroups = playersGroups == null ? new HashSet<>() : playersGroups;
                                playersGroups.remove(group);
                                return nearbyGroups.isEmpty() ? null : playersGroups;
                            });
                        }
                    }
                });
        nearbyGroups.entrySet().removeIf(entry -> entry.getKey().players.size() < 2);
        return new Result(add, remove);
    }

    /**
     * Returns the groups to leave
     * @param player to remove
     */
    public void removePlayerState(Player player) {
        playerHashes.remove(new MemberSource(player, null));
        Set<NearbyGroup> groups = playerToGroups.remove(new MemberSource(player, null));
        if (groups != null) {
            groups.forEach(nearbyGroups::remove);
        }
    }

    public static final class Result {
        public final Map<UUID, NearbyGroup> add;
        public final Map<UUID, NearbyGroup> remove;

        public Result(Map<UUID, NearbyGroup> add, Map<UUID, NearbyGroup> remove) {
            this.add = add;
            this.remove = remove;
        }
    }
}
