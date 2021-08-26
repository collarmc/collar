package com.collarmc.tests.friends;

import com.collarmc.api.friends.Friend;
import com.collarmc.api.friends.Status;
import com.collarmc.client.Collar;
import com.collarmc.client.api.friends.FriendsApi;
import com.collarmc.client.api.friends.FriendsListener;
import com.collarmc.tests.junit.CollarAssert;
import com.collarmc.tests.junit.CollarTest;
import org.junit.Test;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import static com.collarmc.tests.junit.CollarAssert.waitForCondition;

public class FriendsTest extends CollarTest {

    @Test
    public void addRemoveFriendByProfileId() {
        TestFriendListener bobListener = new TestFriendListener();
        bobPlayer.collar.friends().subscribe(bobListener);
        TestFriendListener eveListener = new TestFriendListener();
        evePlayer.collar.friends().subscribe(eveListener);
        TestFriendListener aliceListener = new TestFriendListener();
        alicePlayer.collar.friends().subscribe(aliceListener);

        alicePlayer.collar.friends().addFriend(bobProfile.get().id);
        CollarAssert.waitForCondition("Alice is friends with Bob", () -> alicePlayer.collar.friends().list().stream().anyMatch(friend -> friend.friend.id.equals(bobProfile.get().id)));

        CollarAssert.waitForCondition("Alice was told Bob was added", () -> {
            if (aliceListener.friendAdded.size() < 1) return false;
            Friend friend = aliceListener.friendAdded.getLast();
            return friend != null && friend.status == Status.ONLINE && friend.friend.id.equals(bobProfile.get().id);
        });

        alicePlayer.collar.friends().removeFriend(bobProfile.get().id);
        CollarAssert.waitForCondition("Alice is not friends with Bob", () -> alicePlayer.collar.friends().list().stream().noneMatch(friend -> friend.friend.id.equals(bobProfile.get().id)));

        CollarAssert.waitForCondition("Alice was told Bob was removed", () -> {
            if (aliceListener.friendRemoved.size() < 1) return false;
            Friend friend = aliceListener.friendRemoved.getLast();
            return friend != null && friend.status == Status.ONLINE && friend.friend.id.equals(bobProfile.get().id);
        });
    }

    @Test
    public void addRemoveFriendByPlayerId() {
        TestFriendListener bobListener = new TestFriendListener();
        bobPlayer.collar.friends().subscribe(bobListener);
        TestFriendListener eveListener = new TestFriendListener();
        evePlayer.collar.friends().subscribe(eveListener);
        TestFriendListener aliceListener = new TestFriendListener();
        alicePlayer.collar.friends().subscribe(aliceListener);

        alicePlayer.collar.friends().addFriend(bobPlayer.collar.player().minecraftPlayer);
        CollarAssert.waitForCondition("Alice is friends with Bob", () -> alicePlayer.collar.friends().list().stream().anyMatch(friend -> friend.friend.id.equals(bobProfile.get().id)));

        CollarAssert.waitForCondition("Alice was told Bob was added", () -> {
            if (aliceListener.friendAdded.size() < 1) return false;
            Friend friend = aliceListener.friendAdded.getLast();
            return friend != null && friend.status == Status.ONLINE && friend.friend.id.equals(bobProfile.get().id);
        });

        alicePlayer.collar.friends().removeFriend(bobPlayer.collar.player().minecraftPlayer);
        CollarAssert.waitForCondition("Alice is not friends with Bob", () -> alicePlayer.collar.friends().list().stream().noneMatch(friend -> friend.friend.id.equals(bobProfile.get().id)));

        CollarAssert.waitForCondition("Alice was told Bob was removed", () -> {
            if (aliceListener.friendRemoved.size() < 1) return false;
            Friend friend = aliceListener.friendRemoved.getLast();
            return friend != null && friend.status == Status.ONLINE && friend.friend.id.equals(bobProfile.get().id);
        });
    }

    @Test
    public void statusChangeUpdatesFriendsStatusState() {
        TestFriendListener bobListener = new TestFriendListener();
        bobPlayer.collar.friends().subscribe(bobListener);
        TestFriendListener eveListener = new TestFriendListener();
        evePlayer.collar.friends().subscribe(eveListener);
        TestFriendListener aliceListener = new TestFriendListener();
        alicePlayer.collar.friends().subscribe(aliceListener);

        alicePlayer.collar.friends().addFriend(bobProfile.get().id);
        alicePlayer.collar.friends().addFriend(eveProfile.get().id);

        CollarAssert.waitForCondition("Alice is friends with Bob", () -> alicePlayer.collar.friends().list().stream().anyMatch(friend -> friend.friend.id.equals(bobProfile.get().id)));
        CollarAssert.waitForCondition("Alice is friends with Eve", () -> alicePlayer.collar.friends().list().stream().anyMatch(friend -> friend.friend.id.equals(eveProfile.get().id)));

        // Eve disconnects
        evePlayer.collar.disconnect();

        CollarAssert.waitForCondition("Alice was told Eve is offline", () -> {
            if (aliceListener.friendChanged.size() < 1) return false;
            Friend friend = aliceListener.friendChanged.getLast();
            return friend != null && friend.status == Status.OFFLINE && friend.friend.id.equals(eveProfile.get().id);
        });

        CollarAssert.waitForCondition("Bob didn't know anything about eve", () -> bobListener.friendChanged.isEmpty());
        CollarAssert.waitForCondition("Bob didn't know anything about eve", () -> bobListener.friendChanged.isEmpty());
    }

    @Test
    public void aliceReconnectsWithFriendListPopulated() {
        TestFriendListener bobListener = new TestFriendListener();
        bobPlayer.collar.friends().subscribe(bobListener);
        TestFriendListener eveListener = new TestFriendListener();
        evePlayer.collar.friends().subscribe(eveListener);
        TestFriendListener aliceListener = new TestFriendListener();
        alicePlayer.collar.friends().subscribe(aliceListener);

        alicePlayer.collar.friends().addFriend(bobProfile.get().id);
        alicePlayer.collar.friends().addFriend(eveProfile.get().id);

        CollarAssert.waitForCondition("Alice is friends with Bob", () -> alicePlayer.collar.friends().list().stream().anyMatch(friend -> friend.friend.id.equals(bobProfile.get().id)));
        CollarAssert.waitForCondition("Alice is friends with Eve", () -> alicePlayer.collar.friends().list().stream().anyMatch(friend -> friend.friend.id.equals(eveProfile.get().id)));

        // Alice disconnects and reconnects
        alicePlayer.collar.disconnect();
        waitForCondition("client disconnected", () -> alicePlayer.collar.getState() == Collar.State.DISCONNECTED, 25, TimeUnit.SECONDS);

        alicePlayer.collar.connect();
        waitForCondition("client connected", () -> alicePlayer.collar.getState() == Collar.State.CONNECTED, 25, TimeUnit.SECONDS);

        waitForCondition("has friends", () -> alicePlayer.collar.friends().list().size() == 2);
        CollarAssert.waitForCondition("Alice is friends with Bob", () -> alicePlayer.collar.friends().list().stream().anyMatch(friend -> friend.friend.id.equals(bobProfile.get().id)));
        CollarAssert.waitForCondition("Alice is friends with Eve", () -> alicePlayer.collar.friends().list().stream().anyMatch(friend -> friend.friend.id.equals(eveProfile.get().id)));
    }

    public static class TestFriendListener implements FriendsListener {

        LinkedList<Friend> friendChanged = new LinkedList<>();
        LinkedList<Friend> friendAdded = new LinkedList<>();
        LinkedList<Friend> friendRemoved = new LinkedList<>();

        @Override
        public void onFriendChanged(Collar collar, FriendsApi friendsApi, Friend friend) {
            friendChanged.add(friend);
        }

        @Override
        public void onFriendAdded(Collar collar, FriendsApi friendsApi, Friend added) {
            friendAdded.add(added);
        }

        @Override
        public void onFriendRemoved(Collar collar, FriendsApi friendsApi, Friend removed) {
            friendRemoved.add(removed);
        }
    }
}
