package team.catgirl.collar.tests.friends;

import org.junit.Test;
import team.catgirl.collar.api.friends.Friend;
import team.catgirl.collar.api.friends.Status;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.friends.FriendsApi;
import team.catgirl.collar.client.api.friends.FriendsListener;
import team.catgirl.collar.tests.junit.CollarAssert;
import team.catgirl.collar.tests.junit.CollarTest;

import java.util.LinkedList;

public class FriendsTest extends CollarTest {

    @Test
    public void testAddRemoveFriendByProfileId() {
        TestFriendListener bobListener = new TestFriendListener();
        bobPlayer.collar.friends().subscribe(bobListener);
        TestFriendListener eveListener = new TestFriendListener();
        evePlayer.collar.friends().subscribe(eveListener);
        TestFriendListener aliceListener = new TestFriendListener();
        alicePlayer.collar.friends().subscribe(aliceListener);

        alicePlayer.collar.friends().addFriendByProfileId(bobProfile.get().id);
        CollarAssert.waitForCondition("Alice is friends with Bob", () -> alicePlayer.collar.friends().list().stream().anyMatch(friend -> friend.id.equals(bobProfile.get().id)));

        CollarAssert.waitForCondition("Alice was told Bob was added", () -> {
            if (aliceListener.friendAdded.size() < 1) return false;
            Friend friend = aliceListener.friendAdded.getLast();
            return friend != null && friend.status == Status.ONLINE && friend.id.equals(bobProfile.get().id);
        });

        alicePlayer.collar.friends().removeFriendByProfileId(bobProfile.get().id);
        CollarAssert.waitForCondition("Alice is not friends with Bob", () -> alicePlayer.collar.friends().list().stream().noneMatch(friend -> friend.id.equals(bobProfile.get().id)));

        CollarAssert.waitForCondition("Alice was told Bob was removed", () -> {
            if (aliceListener.friendRemoved.size() < 1) return false;
            Friend friend = aliceListener.friendRemoved.getLast();
            return friend != null && friend.status == Status.ONLINE && friend.id.equals(bobProfile.get().id);
        });
    }

    @Test
    public void testAddRemoveFriendByPlayerId() {
        TestFriendListener bobListener = new TestFriendListener();
        bobPlayer.collar.friends().subscribe(bobListener);
        TestFriendListener eveListener = new TestFriendListener();
        evePlayer.collar.friends().subscribe(eveListener);
        TestFriendListener aliceListener = new TestFriendListener();
        alicePlayer.collar.friends().subscribe(aliceListener);

        alicePlayer.collar.friends().addFriendByPlayerId(bobPlayerId);
        CollarAssert.waitForCondition("Alice is friends with Bob", () -> alicePlayer.collar.friends().list().stream().anyMatch(friend -> friend.id.equals(bobProfile.get().id)));

        CollarAssert.waitForCondition("Alice was told Bob was added", () -> {
            if (aliceListener.friendAdded.size() < 1) return false;
            Friend friend = aliceListener.friendAdded.getLast();
            return friend != null && friend.status == Status.ONLINE && friend.id.equals(bobProfile.get().id);
        });

        alicePlayer.collar.friends().removeFriendByPlayerId(bobPlayerId);
        CollarAssert.waitForCondition("Alice is not friends with Bob", () -> alicePlayer.collar.friends().list().stream().noneMatch(friend -> friend.id.equals(bobProfile.get().id)));

        CollarAssert.waitForCondition("Alice was told Bob was removed", () -> {
            if (aliceListener.friendRemoved.size() < 1) return false;
            Friend friend = aliceListener.friendRemoved.getLast();
            return friend != null && friend.status == Status.ONLINE && friend.id.equals(bobProfile.get().id);
        });
    }

    @Test
    public void testStatusChange() {
        TestFriendListener bobListener = new TestFriendListener();
        bobPlayer.collar.friends().subscribe(bobListener);
        TestFriendListener eveListener = new TestFriendListener();
        evePlayer.collar.friends().subscribe(eveListener);
        TestFriendListener aliceListener = new TestFriendListener();
        alicePlayer.collar.friends().subscribe(aliceListener);

        alicePlayer.collar.friends().addFriendByProfileId(bobProfile.get().id);
        alicePlayer.collar.friends().addFriendByProfileId(eveProfile.get().id);

        CollarAssert.waitForCondition("Alice is friends with Bob", () -> alicePlayer.collar.friends().list().stream().anyMatch(friend -> friend.id.equals(bobProfile.get().id)));
        CollarAssert.waitForCondition("Alice is friends with Eve", () -> alicePlayer.collar.friends().list().stream().anyMatch(friend -> friend.id.equals(eveProfile.get().id)));

        // Eve disconnects
        evePlayer.collar.disconnect();

        CollarAssert.waitForCondition("Alice was told Eve is offline", () -> {
            if (aliceListener.friendChanged.size() < 1) return false;
            Friend friend = aliceListener.friendChanged.getLast();
            return friend != null && friend.status == Status.OFFLINE && friend.id.equals(eveProfile.get().id);
        });

        CollarAssert.waitForCondition("Bob didn't know anything about eve", () -> bobListener.friendChanged.getLast() == null);
        CollarAssert.waitForCondition("Bob didn't know anything about eve", () -> bobListener.friendChanged.getLast() == null);
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
        public void onFriendAdded(Collar collar, FriendsApi friendsApi, Friend friend) {
            friendAdded.add(friend);
        }

        @Override
        public void onFriendRemoved(Collar collar, FriendsApi friendsApi, Friend removed) {
            friendRemoved.add(removed);
        }
    }
}
