package com.collarmc.client.api.location;

import com.collarmc.api.entities.Entity;
import com.collarmc.api.entities.EntityType;
import com.collarmc.api.groups.Group;
import com.collarmc.api.location.Location;
import com.collarmc.api.session.Player;
import com.collarmc.api.waypoints.Waypoint;
import com.collarmc.client.Collar;
import com.collarmc.client.api.AbstractApi;
import com.collarmc.client.api.groups.GroupsApi;
import com.collarmc.client.api.groups.events.GroupLeftEvent;
import com.collarmc.client.api.location.events.*;
import com.collarmc.client.minecraft.Ticks;
import com.collarmc.client.sdht.SDHTApi;
import com.collarmc.client.sdht.event.SDHTRecordAddedEvent;
import com.collarmc.client.sdht.event.SDHTRecordRemovedEvent;
import com.collarmc.client.security.ClientIdentityStore;
import com.collarmc.pounce.Subscribe;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.protocol.location.*;
import com.collarmc.protocol.waypoints.CreateWaypointRequest;
import com.collarmc.protocol.waypoints.GetWaypointsRequest;
import com.collarmc.protocol.waypoints.GetWaypointsResponse;
import com.collarmc.protocol.waypoints.RemoveWaypointRequest;
import com.collarmc.sdht.Content;
import com.collarmc.sdht.Key;
import com.collarmc.security.messages.CipherException;
import com.google.common.collect.ImmutableSet;
import com.google.common.hash.Hashing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class LocationApi extends AbstractApi {

    private static final Logger LOGGER = LogManager.getLogger(LocationApi.class);

    private final HashSet<UUID> groupsSharingWith = new HashSet<>();
    private final ConcurrentHashMap<Player, Location> playerLocations = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Waypoint> privateWaypoints = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Map<UUID, Waypoint>> groupWaypoints = new ConcurrentHashMap<>();
    private final Supplier<Location> locationSupplier;
    private final LocationUpdater updater;
    private final NearbyUpdater nearbyUpdater;
    private final SDHTApi sdhtApi;

    public LocationApi(Collar collar,
                       Supplier<ClientIdentityStore> identityStoreSupplier,
                       Consumer<ProtocolRequest> sender,
                       Ticks ticks,
                       GroupsApi groupsApi,
                       SDHTApi sdhtApi,
                       Supplier<Location> locationSupplier,
                       Supplier<Set<Entity>> entityListSupplier) {
        super(collar, identityStoreSupplier, sender);
        this.locationSupplier = locationSupplier;
        this.updater = new LocationUpdater(this, ticks);
        this.nearbyUpdater = new NearbyUpdater(entityListSupplier, this, ticks);
        this.sdhtApi = sdhtApi;
    }

    /**
     * Players who are sharing their location with you
     * @return players to their locations
     */
    public Map<Player, Location> playerLocations() {
        return new HashMap<>(playerLocations);
    }

    /**
     * @return your private waypoints
     */
    public Set<Waypoint> privateWaypoints() {
        return ImmutableSet.copyOf(privateWaypoints.values());
    }

    /**
     * Waypoints shared with you via a group
     * @param group the waypoints belong to
     * @return waypoints
     */
    public Set<Waypoint> groupWaypoints(Group group) {
        Map<UUID, Waypoint> waypoints = groupWaypoints.get(group.id);
        if (waypoints == null) {
            return ImmutableSet.of();
        }
        return ImmutableSet.copyOf(waypoints.values());
    }

    /**
     * Start sharing your coordinates with a group
     * @param group to share with
     */
    public void startSharingWith(Group group) {
        synchronized (this) {
            groupsSharingWith.add(group.id);
            sender.accept(new StartSharingLocationRequest(group.id));
            if (!updater.isRunning()) {
                updater.start();
            }
        }
        collar.configuration.eventBus.dispatch(new LocationSharingStartedEvent(collar, group));
    }

    /**
     * Start sharing your coordinates with a group
     * @param group to stop sharing with
     */
    public void stopSharingWith(Group group) {
        synchronized (this) {
            stopSharingForGroup(group);
            sender.accept(new StopSharingLocationRequest(group.id));
        }
        collar.configuration.eventBus.dispatch(new LocationSharingStoppedEvent(collar, group));
    }

    /**
     * Tests if you are currently sharing with the group
     * @param group to test
     * @return sharing
     */
    public boolean isSharingWith(Group group) {
        synchronized (this) {
            return groupsSharingWith.contains(group.id);
        }
    }

    /**
     * Tests if you are currently sharing location with one or more groups
     * @return sharing
     */
    public boolean isSharing() {
        return !groupsSharingWith.isEmpty();
    }

    /**
     * Add a shared {@link Waypoint} to the group
     * @param group to add waypoint to
     * @param name of the waypoint
     * @param location of the waypoint
     */
    public void addWaypoint(Group group, String name, Location location) {
        Waypoint waypoint = new Waypoint(UUID.randomUUID(), name, location, collar.player().minecraftPlayer.server);
        groupWaypoints.compute(group.id, (groupId, waypointMap) -> {
            waypointMap = waypointMap == null ? new ConcurrentHashMap<>() : waypointMap;
            waypointMap.computeIfAbsent(waypoint.id, waypointId -> waypoint);
            return waypointMap;
        });
        Content content = Content.from(waypoint.serialize(), Waypoint.class);
        sdhtApi.table.put(new Key(group.id, waypoint.id), content);
        collar.configuration.eventBus.dispatch(new WaypointCreatedEvent(collar, waypoint, group));
    }

    /**
     * Remove a shared {@link Waypoint} from a group
     * @param group to the waypoint belongs to
     * @param waypoint the waypoint to remove
     */
    public void removeWaypoint(Group group, Waypoint waypoint) {
        AtomicReference<Waypoint> removedWaypoint = new AtomicReference<>();
        groupWaypoints.compute(group.id, (uuid, waypointMap) -> {
            if (waypointMap == null) {
                return null;
            }
            Waypoint removed = waypointMap.remove(waypoint.id);
            removedWaypoint.set(removed);
            if (waypointMap.isEmpty()) {
                return null;
            }
            return waypointMap;
        });
        sdhtApi.table.delete(new Key(group.id, waypoint.id));
        if (removedWaypoint.get() != null) {
            collar.configuration.eventBus.dispatch(new WaypointRemovedEvent(collar, waypoint, group));
        }
    }

    /**
     * Add a {@link Waypoint} to the players private waypoint list
     * @param name of the waypoint
     * @param location of the waypoint
     */
    public void addWaypoint(String name, Location location) {
        Waypoint waypoint = privateWaypoints.computeIfAbsent(UUID.randomUUID(), uuid -> new Waypoint(uuid, name, location, collar.player().minecraftPlayer.server));
        byte[] bytes = waypoint.serialize();
        byte[] encryptedBytes;
        try {
            encryptedBytes = identityStore().cipher().encrypt(bytes);
        } catch (CipherException e) {
            throw new IllegalStateException(e);
        }
        sender.accept(new CreateWaypointRequest(waypoint.id, encryptedBytes));
        collar.configuration.eventBus.dispatch(new WaypointCreatedEvent(collar, waypoint, null));
    }

    /**
     * Remove a {@link Waypoint} from the players private waypoint list
     * @param waypoint to remove
     */
    public void removeWaypoint(Waypoint waypoint) {
        privateWaypoints.remove(waypoint.id);
        sender.accept(new RemoveWaypointRequest(waypoint.id));
        collar.configuration.eventBus.dispatch(new WaypointRemovedEvent(collar, waypoint, null));
    }

    private void stopSharingForGroup(Group group) {
        synchronized (this) {
            if (updater.isRunning() && groupsSharingWith.contains(group.id) && (groupsSharingWith.size() - 1) == 0) {
                updater.stop();
            }
            groupsSharingWith.remove(group.id);
        }
    }

    void publishLocation() {
        if (!groupsSharingWith.isEmpty()) {
            Location location = locationSupplier.get();
            byte[] bytes = location.serialize();
            groupsSharingWith.forEach(groupId -> {
                collar.groups().findGroupById(groupId)
                        .flatMap(group -> identityStore().groupSessions().session(group))
                        .ifPresent(groupSession -> {
                            byte[] encryptedBytes;
                            try {
                                encryptedBytes = groupSession.encrypt(bytes);
                            } catch (CipherException e) {
                                LOGGER.error("Could not share location with group " + groupId, e);
                                return;
                            }
                            sender.accept(new UpdateLocationRequest(groupId, encryptedBytes));
                        });
            });
        }
    }

    void publishNearby(Set<Entity> entities) {
        if (collar.getState() != Collar.State.CONNECTED) {
            return;
        }
        Set<String> nearbyHashes = entities.stream().filter(entity -> entity.isTypeOf(EntityType.PLAYER))
                .limit(200)
                .map(entity -> Hashing.sha256().hashString(entity.id.toString(), StandardCharsets.UTF_8).toString())
                .sorted()
                .collect(Collectors.toSet());
        sender.accept(new UpdateNearbyRequest(nearbyHashes));
    }

    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        if (resp instanceof LocationUpdatedResponse) {
            LocationUpdatedResponse response = (LocationUpdatedResponse) resp;
            synchronized (this) {
                collar.groups().findGroupById(response.group).ifPresent(group -> {
                    Optional<Location> location = identityStore().groupSessions().session(group).map(groupSession -> {
                        if (response.location == null) {
                            return null;
                        }
                        try {
                            byte[] contents = groupSession.decrypt(response.location, response.sender.identity);
                            return new Location(contents);
                        } catch (IOException | CipherException e) {
                            LOGGER.error("could not decrypt location sent by " + response.sender.identity);
                            return null;
                        }
                    });
                    if (location.isPresent()) {
                        // Update the location
                        playerLocations.put(response.sender, location.get());
                    } else {
                        // Remove if stopped sharing
                        playerLocations.remove(response.sender);
                    }
                    collar.configuration.eventBus.dispatch(new LocationUpdatedEvent(collar, response.sender, location.orElse(Location.UNKNOWN)));
                });
            }
            return true;
        } else if (resp instanceof GetWaypointsResponse) {
            GetWaypointsResponse response = (GetWaypointsResponse) resp;
            if (!response.waypoints.isEmpty()) {
                Map<UUID, Waypoint> waypoints = response.waypoints.stream()
                        .map(encryptedWaypoint -> {
                            try {
                                return identityStore().cipher().decrypt(encryptedWaypoint.waypoint);
                            } catch (CipherException e) {
                                throw new IllegalStateException("Unable to decrypt private waypoint", e);
                            }
                        }).map(Waypoint::new)
                        .filter(waypoint -> waypoint.server.equals(collar.player().minecraftPlayer.server))
                        .collect(Collectors.toMap(o -> o.id, o -> o));
                privateWaypoints.putAll(waypoints);
                collar.configuration.eventBus.dispatch(new PrivateWaypointsReceivedEvent(collar, ImmutableSet.copyOf(waypoints.values())));
            }
        }
        return false;
    }

    @Subscribe
    public void onGroupLeft(GroupLeftEvent e) {
        stopSharingForGroup(e.group);
    }

    public void onRecordAdded(Collar collar, SDHTApi sdhtApi, Key key, Content content) {
        if (Waypoint.class.equals(content.type)) {
            collar.groups().findGroupById(key.namespace).ifPresent(group -> {
                AtomicReference<Waypoint> waypointAdded = new AtomicReference<>();
                groupWaypoints.compute(key.namespace, (uuid, waypointMap) -> {
                    waypointMap = waypointMap == null ? new ConcurrentHashMap<>() : waypointMap;
                    Waypoint waypoint = new Waypoint(content.bytes);
                    waypointMap.put(key.id, waypoint);
                    waypointAdded.set(waypoint);
                    return waypointMap;
                });
                if (waypointAdded.get() != null) {
                    collar.configuration.eventBus.dispatch(new WaypointCreatedEvent(collar, waypointAdded.get(), group));
                }
            });
        }
    }

    @Subscribe
    public void onRecordRemoved(Collar collar, SDHTApi sdhtApi, Key key, Content content) {
        if (Waypoint.class.equals(content.type)) {
            collar.groups().findGroupById(key.namespace).ifPresent(group -> {
                AtomicReference<Waypoint> waypointRemoved = new AtomicReference<>();
                groupWaypoints.compute(key.namespace, (uuid, waypointMap) -> {
                    if (waypointMap == null) {
                        return null;
                    }
                    Waypoint removed = waypointMap.remove(key.id);
                    waypointRemoved.set(removed);
                    return waypointMap.isEmpty() ? null : waypointMap;
                });
                if (waypointRemoved.get() != null) {
                    collar.configuration.eventBus.dispatch(new WaypointRemovedEvent(collar, waypointRemoved.get(), group));
                }
            });
        }
    }

    @Override
    public void onStateChanged(Collar.State state) {
        if (state == Collar.State.CONNECTED) {
            nearbyUpdater.start();
            sender.accept(new GetWaypointsRequest());
        } else if (state == Collar.State.DISCONNECTED) {
            synchronized (this) {
                updater.stop();
                playerLocations.clear();
                groupsSharingWith.clear();
            }
            nearbyUpdater.stop();
        }
    }

    @Subscribe
    public void onRecordAdded(SDHTRecordAddedEvent event) {
        if (Waypoint.class.equals(event.content.type)) {
            collar.groups().findGroupById(event.key.namespace).ifPresent(group -> {
                AtomicReference<Waypoint> waypointAdded = new AtomicReference<>();
                groupWaypoints.compute(event.key.namespace, (uuid, waypointMap) -> {
                    waypointMap = waypointMap == null ? new ConcurrentHashMap<>() : waypointMap;
                    Waypoint waypoint = new Waypoint(event.content.bytes);
                    waypointMap.put(event.key.id, waypoint);
                    waypointAdded.set(waypoint);
                    return waypointMap;
                });
                if (waypointAdded.get() != null) {
                    collar.configuration.eventBus.dispatch(new WaypointCreatedEvent(collar, waypointAdded.get(), group));
                }
            });
        }
    }

    @Subscribe
    public void onRecordRemoved(SDHTRecordRemovedEvent event) {
        if (Waypoint.class.equals(event.content.type)) {
            collar.groups().findGroupById(event.key.namespace).ifPresent(group -> {
                AtomicReference<Waypoint> waypointRemoved = new AtomicReference<>();
                groupWaypoints.compute(event.key.namespace, (uuid, waypointMap) -> {
                    if (waypointMap == null) {
                        return null;
                    }
                    Waypoint removed = waypointMap.remove(event.key.id);
                    waypointRemoved.set(removed);
                    return waypointMap.isEmpty() ? null : waypointMap;
                });
                if (waypointRemoved.get() != null) {
                    collar.configuration.eventBus.dispatch(new WaypointRemovedEvent(collar, waypointRemoved.get(), group));
                }
            });
        }
    }
}
