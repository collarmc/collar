package com.collarmc.tests.location;

import com.collarmc.api.groups.Group;
import com.collarmc.api.groups.GroupType;
import com.collarmc.api.location.Dimension;
import com.collarmc.api.location.Location;
import com.collarmc.api.session.Player;
import com.collarmc.api.waypoints.Waypoint;
import com.collarmc.client.Collar;
import com.collarmc.client.api.groups.events.GroupInvitationEvent;
import com.collarmc.client.api.groups.events.GroupJoinedEvent;
import com.collarmc.client.api.location.events.LocationUpdatedEvent;
import com.collarmc.client.api.location.events.PrivateWaypointsReceivedEvent;
import com.collarmc.client.api.location.events.WaypointCreatedEvent;
import com.collarmc.client.api.location.events.WaypointRemovedEvent;
import com.collarmc.pounce.EventBus;
import com.collarmc.pounce.Subscribe;
import com.collarmc.tests.groups.GroupsTest;
import com.collarmc.tests.junit.CollarTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.collarmc.tests.junit.CollarAssert.waitForCondition;

public class LocationTest extends CollarTest {

    @Test
    public void startAndStopSharing() {
        LocationSharingTestGroupListener aliceGroupListener = new LocationSharingTestGroupListener(alicePlayer.eventBus);
        LocationSharingListener aliceLocationListener = new LocationSharingListener(alicePlayer.eventBus);

        LocationSharingTestGroupListener bobGroupListener = new LocationSharingTestGroupListener(bobPlayer.eventBus);
        LocationSharingListener bobLocationListener = new LocationSharingListener(bobPlayer.eventBus);

        LocationSharingTestGroupListener eveGroupListener = new LocationSharingTestGroupListener(evePlayer.eventBus);
        LocationSharingListener eveLocationListener = new LocationSharingListener(evePlayer.eventBus);

        // Put alice and bob in a group
        alicePlayer.collar.groups().create("cute group", GroupType.PARTY, List.of(bobPlayerId));

        // Put eve in her own group
        evePlayer.collar.groups().create("not cute group", GroupType.PARTY, List.of());

        waitForCondition("Alice joined group", () -> aliceGroupListener.group != null);
        waitForCondition("Bob joined group", () -> bobGroupListener.group != null);
        waitForCondition("Eve joined group", () -> eveGroupListener.group != null);

        // Make sure they aren't in the same group
        Assert.assertNotEquals(aliceGroupListener.group.id, eveGroupListener.group.id);
        Assert.assertNotEquals(bobGroupListener.group.id, eveGroupListener.group.id);
        Assert.assertEquals(bobGroupListener.group.id, aliceGroupListener.group.id);

        Group aliceBobGroup = aliceGroupListener.group;
        alicePlayer.collar.location().startSharingWith(aliceBobGroup);
        bobPlayer.collar.location().startSharingWith(aliceBobGroup);
        evePlayer.collar.location().startSharingWith(eveGroupListener.group);

        Assert.assertTrue(alicePlayer.collar.location().isSharingWith(aliceBobGroup));
        Assert.assertTrue(bobPlayer.collar.location().isSharingWith(aliceBobGroup));
        Assert.assertFalse(evePlayer.collar.location().isSharingWith(aliceBobGroup));
        Assert.assertTrue(evePlayer.collar.location().isSharingWith(eveGroupListener.group));

        waitForCondition("Alice has sent Bob her location", () -> bobPlayer.collar.location().playerLocations().containsKey(alicePlayer.collar.player()), 20, TimeUnit.SECONDS);
        waitForCondition("Bob has sent Alice her location", () -> alicePlayer.collar.location().playerLocations().containsKey(bobPlayer.collar.player()), 20, TimeUnit.SECONDS);
        waitForCondition("Eve doesn't know bob's location", () -> !evePlayer.collar.location().playerLocations().containsKey(bobPlayer.collar.player()), 20, TimeUnit.SECONDS);
        waitForCondition("Eve doesn't know alice's location", () -> !evePlayer.collar.location().playerLocations().containsKey(alicePlayer.collar.player()), 20, TimeUnit.SECONDS);

        Assert.assertNotEquals("last event should be not be unknown location", bobLocationListener.events.getLast().location, Location.UNKNOWN);
        Assert.assertNotEquals("last event should be not be unknown location", aliceLocationListener.events.getLast().location, Location.UNKNOWN);

        alicePlayer.collar.location().stopSharingWith(aliceBobGroup);
        bobPlayer.collar.location().stopSharingWith(aliceBobGroup);
        evePlayer.collar.location().stopSharingWith(eveGroupListener.group);

        Assert.assertFalse(alicePlayer.collar.location().isSharingWith(aliceBobGroup));
        Assert.assertFalse(bobPlayer.collar.location().isSharingWith(aliceBobGroup));
        Assert.assertFalse(evePlayer.collar.location().isSharingWith(aliceBobGroup));
        Assert.assertFalse(evePlayer.collar.location().isSharingWith(eveGroupListener.group));

        waitForCondition("Bob shouldn't have alice's location after she has stopped sharing", () -> !bobPlayer.collar.location().playerLocations().containsKey(alicePlayer.collar.player()), 20, TimeUnit.SECONDS);
        waitForCondition("Alice shouldn't have bob's location after he has stopped sharing", () -> !alicePlayer.collar.location().playerLocations().containsKey(bobPlayer.collar.player()), 20, TimeUnit.SECONDS);

        Assert.assertEquals("last event should be unknown location", bobLocationListener.events.getLast().location, Location.UNKNOWN);
        Assert.assertEquals("last event should be unknown location", aliceLocationListener.events.getLast().location, Location.UNKNOWN);
    }

    @Test
    public void privateWaypointsCreateAndDelete() {
        Assert.assertTrue(alicePlayer.collar.location().privateWaypoints().isEmpty());
        Location location = new Location(-3000d, 64d, -3000d, Dimension.OVERWORLD);
        alicePlayer.collar.location().addWaypoint("My base", location);
        Assert.assertFalse(alicePlayer.collar.location().privateWaypoints().isEmpty());
        Waypoint waypoint = alicePlayer.collar.location().privateWaypoints().iterator().next();
        Assert.assertEquals("My base", waypoint.name);
        Assert.assertEquals(location, waypoint.location);
        alicePlayer.collar.location().removeWaypoint(waypoint);
        Assert.assertTrue(alicePlayer.collar.location().privateWaypoints().isEmpty());
    }

    @Test
    public void privateWaypointsArriveOnConnect() {
        PrivateWaypointListener listener = new PrivateWaypointListener(alicePlayer.eventBus);
        Assert.assertTrue(alicePlayer.collar.location().privateWaypoints().isEmpty());
        Location location = new Location(-3000d, 64d, -3000d, Dimension.OVERWORLD);
        alicePlayer.collar.location().addWaypoint("My base", location);
        alicePlayer.collar.location().addWaypoint("Cute base", location);
        Assert.assertEquals(2, alicePlayer.collar.location().privateWaypoints().size());

        alicePlayer.collar.disconnect();
        waitForCondition("alice disconnected", () -> alicePlayer.collar.getState() == Collar.State.DISCONNECTED);
        alicePlayer.collar.connect();
        waitForCondition("alice connected", () -> alicePlayer.collar.getState() == Collar.State.CONNECTED, 25, TimeUnit.SECONDS);
        waitForCondition("has waypoints", () -> listener.privateWaypoints != null);

        Assert.assertTrue(listener.privateWaypoints.stream().anyMatch(waypoint -> waypoint.name.equals("My base") && waypoint.location.equals(location)));
        Assert.assertTrue(listener.privateWaypoints.stream().anyMatch(waypoint -> waypoint.name.equals("Cute base") && waypoint.location.equals(location)));
    }

    @Test
    public void groupWaypointsCreateAndDelete() {

        WaypointListener aliceWaypointListener = new WaypointListener(alicePlayer.eventBus);
        GroupsTest.TestGroupsListener bobListener = new GroupsTest.TestGroupsListener(bobPlayer.eventBus);
        WaypointListener bobWaypointListener = new WaypointListener(bobPlayer.eventBus);
        GroupsTest.TestGroupsListener eveListener = new GroupsTest.TestGroupsListener(evePlayer.eventBus);
        WaypointListener eveWaypointListener = new WaypointListener(evePlayer.eventBus);

        // Alice creates a new group with bob and eve
        alicePlayer.collar.groups().create("cute group", GroupType.PARTY, List.of(bobPlayerId, evePlayerId));

        // Check that Eve and Bob received their invitations
        waitForCondition("Eve invite received", () -> eveListener.invitation != null);
        waitForCondition("Bob invite received", () -> bobListener.invitation != null);

        // Accept the invitation
        bobPlayer.collar.groups().accept(bobListener.invitation);
        evePlayer.collar.groups().accept(eveListener.invitation);

        waitForCondition("Eve joined group", () -> eveListener.joinedGroup);
        waitForCondition("Bob joined group", () -> bobListener.joinedGroup);

        Group theGroup = alicePlayer.collar.groups().all().get(0);

        // Check there are zero waypoints
        waitForCondition("alice does not have a waypoint", () -> alicePlayer.collar.location().groupWaypoints(theGroup).isEmpty());
        waitForCondition("bob does not have a waypoint", () -> bobPlayer.collar.location().groupWaypoints(theGroup).isEmpty());
        waitForCondition("bob does not have a waypoint", () -> evePlayer.collar.location().groupWaypoints(theGroup).isEmpty());

        // Bob creates a waypoint
        Location baseLocation = new Location(0d, 64d, 0d, Dimension.OVERWORLD);
        bobPlayer.collar.location().addWaypoint(theGroup, "Our base", baseLocation);
        System.err.println("added waypoint");

        waitForCondition("alice has a waypoint", () -> !alicePlayer.collar.location().groupWaypoints(theGroup).isEmpty(), 10, TimeUnit.SECONDS);
        waitForCondition("bob has a waypoint", () -> !bobPlayer.collar.location().groupWaypoints(theGroup).isEmpty(), 10, TimeUnit.SECONDS);
        waitForCondition("eve has a waypoint", () -> !evePlayer.collar.location().groupWaypoints(theGroup).isEmpty(), 10, TimeUnit.SECONDS);

        waitForCondition("alice has the waypoint", () -> {
            Waypoint waypoint = alicePlayer.collar.location().groupWaypoints(theGroup).stream().filter(waypoint1 -> waypoint1.equals(aliceWaypointListener.lastWaypointCreated)).findFirst().orElse(null);
            return waypoint != null;
        });
        waitForCondition("bob has the waypoint", () -> {
            Waypoint waypoint = bobPlayer.collar.location().groupWaypoints(theGroup).stream().filter(waypoint1 -> waypoint1.equals(bobWaypointListener.lastWaypointCreated)).findFirst().orElse(null);
            return waypoint != null;
        });

        waitForCondition("eve has the waypoint", () -> {
            Waypoint waypoint = evePlayer.collar.location().groupWaypoints(theGroup).stream().filter(waypoint1 -> waypoint1.equals(eveWaypointListener.lastWaypointCreated)).findFirst().orElse(null);
            return waypoint != null;
        });

        alicePlayer.collar.location().removeWaypoint(theGroup, aliceWaypointListener.lastWaypointCreated);

        waitForCondition("alice does not have a waypoint", () -> alicePlayer.collar.location().groupWaypoints(theGroup).isEmpty());
        waitForCondition("eve does not have a waypoint", () -> evePlayer.collar.location().groupWaypoints(theGroup).isEmpty());
        waitForCondition("bob does not have a waypoint", () -> bobPlayer.collar.location().groupWaypoints(theGroup).isEmpty());
    }

    @Test
    public void newGroupMemberReceivesWaypoints() throws Exception {
        WaypointListener aliceWaypointListener = new WaypointListener(alicePlayer.eventBus);
        GroupsTest.TestGroupsListener bobListener = new GroupsTest.TestGroupsListener(bobPlayer.eventBus);
        WaypointListener bobWaypointListener = new WaypointListener(bobPlayer.eventBus);
        GroupsTest.TestGroupsListener eveListener = new GroupsTest.TestGroupsListener(evePlayer.eventBus);
        WaypointListener eveWaypointListener = new WaypointListener(evePlayer.eventBus);

        // Alice creates a new group with bob and eve
        alicePlayer.collar.groups().create("cute group", GroupType.PARTY, List.of(bobPlayerId));

        // Check that Eve and Bob received their invitations
        waitForCondition("Bob invite received", () -> bobListener.invitation != null);
        waitForCondition("Eve invite received", () -> eveListener.invitation == null);

        // Accept the invitation
        bobPlayer.collar.groups().accept(bobListener.invitation);

        // Bob joins the group
        waitForCondition("Bob joined group", () -> bobListener.joinedGroup);

        Group theGroup = bobPlayer.collar.groups().all().get(0);
        // Bob creates a waypoint
        Location baseLocation = new Location(0d, 64d, 0d, Dimension.OVERWORLD);
        bobPlayer.collar.location().addWaypoint(theGroup, "Our base", baseLocation);

        waitForCondition("alice has the waypoint", () -> {
            Waypoint waypoint = alicePlayer.collar.location().groupWaypoints(theGroup)
                    .stream().filter(candidate -> candidate.equals(aliceWaypointListener.lastWaypointCreated))
                    .findFirst().orElse(null);
            return waypoint != null;
        });
        waitForCondition("bob has the waypoint", () -> {
            Waypoint waypoint = bobPlayer.collar.location().groupWaypoints(theGroup).stream()
                    .filter(candidate -> candidate.equals(bobWaypointListener.lastWaypointCreated))
                    .findFirst().orElse(null);
            return waypoint != null;
        });

        // Eve is invited to the group and accepts
        alicePlayer.collar.groups().invite(theGroup, evePlayerId);
        waitForCondition("Eve invite received", () -> eveListener.invitation != null);
        evePlayer.collar.groups().accept(eveListener.invitation);
        waitForCondition("Eve joined group", () -> eveListener.joinedGroup);

        // Eve should have received the waypoint when her SDHT syncs with the groups namespace
        waitForCondition("eve has the waypoint", () -> {
            Waypoint waypoint = evePlayer.collar.location().groupWaypoints(theGroup).stream()
                    .filter(candidate -> candidate.equals(eveWaypointListener.lastWaypointCreated))
                    .findFirst().orElse(null);
            return waypoint != null;
        });
    }

    public static class Event {
        public final Player player;
        public final Location location;

        public Event(Player player, Location location) {
            this.player = player;
            this.location = location;
        }
    }

    public static class WaypointListener {
        public Waypoint lastWaypointCreated;
        public Waypoint lastWaypointDeleted;

        public WaypointListener(EventBus eventBus) {
            eventBus.subscribe(this);
        }

        @Subscribe
        public void onWaypointCreated(WaypointCreatedEvent event) {
            this.lastWaypointCreated = event.waypoint;
        }

        @Subscribe
        public void onWaypointRemoved(WaypointRemovedEvent event) {
            this.lastWaypointDeleted = event.waypoint;
        }
    }

    public static class LocationSharingListener {

        public final LinkedList<Event> events = new LinkedList<>();

        public LocationSharingListener(EventBus eventBus) {
            eventBus.subscribe(this);
        }

        @Subscribe
        public void onLocationUpdated(LocationUpdatedEvent event) {
            events.add(new Event(event.player, event.location));
        }
    }

    public static class LocationSharingTestGroupListener {

        public Group group;

        public LocationSharingTestGroupListener(EventBus eventBus) {
            eventBus.subscribe(this);
        }

        @Subscribe
        public void onGroupJoined(GroupJoinedEvent event) {
            this.group = event.group;
        }

        @Subscribe
        public void onGroupInvited(GroupInvitationEvent event) {
            event.collar.groups().accept(event.invitation);
        }
    }

    public static class PrivateWaypointListener {
        public Set<Waypoint> privateWaypoints;

        public PrivateWaypointListener(EventBus eventBus) {
            eventBus.subscribe(this);
        }

        @Subscribe
        public void onPrivateWaypointsReceived(PrivateWaypointsReceivedEvent event) {
            this.privateWaypoints = event.privateWaypoints;
        }
    }
}
