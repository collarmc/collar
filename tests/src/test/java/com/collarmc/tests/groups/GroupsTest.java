package com.collarmc.tests.groups;

import com.collarmc.api.groups.Group;
import com.collarmc.api.groups.GroupType;
import com.collarmc.api.groups.Member;
import com.collarmc.api.groups.MembershipRole;
import com.collarmc.api.messaging.Message;
import com.collarmc.api.messaging.TextMessage;
import com.collarmc.client.Collar;
import com.collarmc.client.api.groups.GroupInvitation;
import com.collarmc.client.api.groups.events.GroupCreatedEvent;
import com.collarmc.client.api.groups.events.GroupInvitationEvent;
import com.collarmc.client.api.groups.events.GroupJoinedEvent;
import com.collarmc.client.api.groups.events.GroupLeftEvent;
import com.collarmc.client.api.messaging.events.GroupMessageReceivedEvent;
import com.collarmc.client.api.messaging.events.GroupMessageSentEvent;
import com.collarmc.pounce.EventBus;
import com.collarmc.pounce.Subscribe;
import com.collarmc.tests.junit.CollarTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.collarmc.tests.junit.CollarAssert.waitForCondition;

public class GroupsTest extends CollarTest {

    TestGroupsListener aliceListener;
    TestGroupsListener bobListener;
    TestGroupsListener eveListener;

    @Before
    public void createListeners() {
        aliceListener = new TestGroupsListener(alicePlayer.eventBus);
        bobListener = new TestGroupsListener(bobPlayer.eventBus);
        eveListener = new TestGroupsListener(evePlayer.eventBus);
    }

    @Test
    public void createGroupAndPerformMembershipActions() {

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

        // Find eve
        Member eveMember = theGroup.members.stream().filter(candidate -> candidate.player.equals(evePlayer.collar.player())).findFirst().orElseThrow();

        waitForCondition("eve is in alice's group", () -> alicePlayer.collar.groups().all().get(0).containsPlayer(evePlayer.collar.player()));
        waitForCondition("eve is in bobs's group", () -> bobPlayer.collar.groups().all().get(0).containsPlayer(evePlayer.collar.player()));
        waitForCondition("eve is in eve's group", () -> evePlayer.collar.groups().all().get(0).containsPlayer(evePlayer.collar.player()));

        waitForCondition("alice is in alice's group", () -> alicePlayer.collar.groups().all().get(0).containsPlayer(alicePlayer.collar.player()));
        waitForCondition("alice is in bobs's group", () -> bobPlayer.collar.groups().all().get(0).containsPlayer(alicePlayer.collar.player()));
        waitForCondition("alice is in eve's group", () -> evePlayer.collar.groups().all().get(0).containsPlayer(alicePlayer.collar.player()));

        waitForCondition("bob is in alice's group", () -> alicePlayer.collar.groups().all().get(0).containsPlayer(bobPlayer.collar.player()));
        waitForCondition("bob is in bobs's group", () -> bobPlayer.collar.groups().all().get(0).containsPlayer(bobPlayer.collar.player()));
        waitForCondition("bob is in eve's group", () -> evePlayer.collar.groups().all().get(0).containsPlayer(bobPlayer.collar.player()));

        // Remove eve
        alicePlayer.collar.groups().removeMember(theGroup, eveMember);

        waitForCondition("eve is no longer a member", () -> evePlayer.collar.groups().all().isEmpty());
        waitForCondition("eve is no longer in alice's group state", () -> !alicePlayer.collar.groups().all().get(0).containsPlayer(evePlayer.collar.player()));
        waitForCondition("eve is no longer in bob's group state", () -> !bobPlayer.collar.groups().all().get(0).containsPlayer(evePlayer.collar.player()));

        // Alice leaves the group
        alicePlayer.collar.groups().leave(theGroup);

        waitForCondition("alice is no longer a member", () -> alicePlayer.collar.groups().all().isEmpty());
        waitForCondition("alice is no longer in bob's group state", () -> !bobPlayer.collar.groups().all().get(0).containsPlayer(alicePlayer.collar.player()));

        bobPlayer.collar.groups().leave(theGroup);
        waitForCondition("bob is no longer a member", () -> bobPlayer.collar.groups().all().isEmpty());
        waitForCondition("eve is no longer a member", () -> evePlayer.collar.groups().all().isEmpty());
        waitForCondition("alice is no longer a member", () -> alicePlayer.collar.groups().all().isEmpty());
    }

    @Test
    public void canRejoinGroupWhenBackOnline() {
        // Alice creates a new group with bob and eve
        alicePlayer.collar.groups().create("cute group", GroupType.PARTY, List.of(bobPlayerId));

        // Check that Eve and Bob received their invitations
        waitForCondition("Bob invite received", () -> bobListener.invitation != null);

        // Accept the invitation
        bobPlayer.collar.groups().accept(bobListener.invitation);

        waitForCondition("Bob joined group", () -> bobListener.joinedGroup);

        Group theGroup = alicePlayer.collar.groups().all().get(0);

        bobPlayer.collar.disconnect();

        waitForCondition("disconnected", () -> bobPlayer.collar.getState() == Collar.State.DISCONNECTED);

        waitForCondition("bob should not have minecraft player", () -> {
            Group group = alicePlayer.collar.groups().findGroupById(theGroup.id).orElse(null);
            if (group == null) return false;
            Member bobMember = group.members.stream().filter(member -> member.player.identity.id().equals(bobProfile.get().id)).findFirst().orElseThrow();
            return bobMember.player.minecraftPlayer == null;
        });

        bobPlayer.collar.connect();

        waitForCondition("bob connected", () -> bobPlayer.collar.getState() == Collar.State.CONNECTED, 30, TimeUnit.SECONDS);

        waitForCondition("bob should not have minecraft player", () -> {
            Group group = alicePlayer.collar.groups().findGroupById(theGroup.id).orElse(null);
            if (group == null) return false;
            Member bobMember = group.members.stream().filter(member -> member.player.identity.id().equals(bobProfile.get().id)).findFirst().orElseThrow();
            return bobMember.player.minecraftPlayer == null;
        });
    }

    @Test
    public void pendingInvitationsResentWhenComingOnline() {
        // Alice creates a new group with bob and eve
        alicePlayer.collar.groups().create("cute group", GroupType.PARTY, List.of(bobPlayerId));

        // Check that Eve and Bob received their invitations
        waitForCondition("Bob invite received", () -> bobListener.invitation != null);
        Group theGroup = alicePlayer.collar.groups().all().get(0);
        Assert.assertEquals(theGroup.id, bobListener.invitation.group);

        bobPlayer.collar.disconnect();

        waitForCondition("disconnected", () -> bobPlayer.collar.getState() == Collar.State.DISCONNECTED);

        waitForCondition("bob should not have minecraft player", () -> {
            Group group = alicePlayer.collar.groups().findGroupById(theGroup.id).orElse(null);
            if (group == null) return false;
            Member bobMember = group.members.stream().filter(member -> member.player.identity.id().equals(bobProfile.get().id)).findFirst().orElseThrow();
            return bobMember.player.minecraftPlayer == null;
        });

        bobPlayer.collar.connect();

        waitForCondition("bob connected", () -> bobPlayer.collar.getState() == Collar.State.CONNECTED, 30, TimeUnit.SECONDS);

        // Check that Eve and Bob received their invitations
        waitForCondition("Bob invite received", () -> bobListener.invitation != null);
        Assert.assertEquals(theGroup.id, bobListener.invitation.group);
    }

    @Test
    public void transferGroup() {

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

        alicePlayer.collar.groups().transferOwnership(theGroup, bobPlayer.collar.player());

        waitForCondition("alice is member", () -> {
            Group group = alicePlayer.collar.groups().findGroupById(theGroup.id).orElse(null);
            if (group == null) return false;
            return group.getRole(alicePlayer.collar.player()) == MembershipRole.MEMBER;
        }, 20, TimeUnit.SECONDS);

        waitForCondition("bob is owner", () -> {
            Group group = bobPlayer.collar.groups().findGroupById(theGroup.id).orElse(null);
            return group != null && group.getRole(bobPlayer.collar.player()) == MembershipRole.OWNER;
        }, 20, TimeUnit.SECONDS);
    }

    @Test
    public void deleteGroup() {

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

        alicePlayer.collar.groups().delete(theGroup);

        waitForCondition("bob is no longer a member", () -> bobPlayer.collar.groups().all().isEmpty());
        waitForCondition("eve is no longer a member", () -> evePlayer.collar.groups().all().isEmpty());
    }

    @Test
    public void sendGroupMessage() {
        MessagingListenerImpl aliceMessages = new MessagingListenerImpl(alicePlayer.eventBus);
        MessagingListenerImpl bobMessages = new MessagingListenerImpl(bobPlayer.eventBus);
        MessagingListenerImpl eveMessages = new MessagingListenerImpl(evePlayer.eventBus);

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

        alicePlayer.collar.messaging().sendGroupMessage(theGroup, new TextMessage("UwU"));
        waitForCondition("bob receives UwU", () -> bobMessages.lastReceivedMessage instanceof TextMessage && "UwU".equals(((TextMessage) bobMessages.lastReceivedMessage).content));
        waitForCondition("eve receives UwU", () -> eveMessages.lastReceivedMessage instanceof TextMessage && "UwU".equals(((TextMessage) eveMessages.lastReceivedMessage).content));
        waitForCondition("alice did not get UwU", () -> aliceMessages.lastReceivedMessage == null);

        bobPlayer.collar.messaging().sendGroupMessage(theGroup, new TextMessage("OwO"));
        waitForCondition("eve receives OwO", () -> eveMessages.lastReceivedMessage instanceof TextMessage && "OwO".equals(((TextMessage) eveMessages.lastReceivedMessage).content));
        waitForCondition("alice receives OwO", () -> aliceMessages.lastReceivedMessage instanceof TextMessage && "OwO".equals(((TextMessage) aliceMessages.lastReceivedMessage).content));
        waitForCondition("bobs last message was UwU", () -> bobMessages.lastReceivedMessage instanceof TextMessage && "UwU".equals(((TextMessage) bobMessages.lastReceivedMessage).content));
    }

    public static class TestGroupsListener {

        public boolean createdGroup = false;
        public boolean joinedGroup = false;
        public boolean leftGroup = false;
        public GroupInvitation invitation;

        public TestGroupsListener(EventBus eventBus) {
            eventBus.subscribe(this);
        }

        @Subscribe
        public void onGroupCreated(GroupCreatedEvent ignored) {
            createdGroup = true;
        }

        @Subscribe
        public void onGroupJoined(GroupJoinedEvent event) {
            if (event.collar.player().equals(event.player)) {
                joinedGroup = true;
            }
        }

        @Subscribe
        public void onGroupLeft(GroupLeftEvent event) {
            if (event.collar.player().equals(event.player)) {
                leftGroup = true;
            }
        }

        @Subscribe
        public void onGroupInvited(GroupInvitationEvent event) {
            this.invitation = event.invitation;
        }
    }

    public static class MessagingListenerImpl {
        public Message lastSentMessage;
        public Message lastReceivedMessage;

        public MessagingListenerImpl(EventBus eventBus) {
            eventBus.subscribe(this);
        }

        @Subscribe
        public void onGroupMessageSent(GroupMessageSentEvent event) {
            this.lastSentMessage = event.message;
        }

        @Subscribe
        public void onGroupMessageReceived(GroupMessageReceivedEvent event) {
            this.lastReceivedMessage = event.message;
        }
    }
}
