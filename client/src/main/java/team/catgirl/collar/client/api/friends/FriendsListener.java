package team.catgirl.collar.client.api.friends;

import team.catgirl.collar.api.friends.Friend;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.ApiListener;

public interface FriendsListener extends ApiListener {
    /**
     * Fired when the local state of a friend has changed (e.g. online, offline, loading friends list)
     * @param collar client
     * @param friendsApi friends
     * @param friend changed
     */
    default void onFriendChanged(Collar collar, FriendsApi friendsApi, Friend friend) {}

    /**
     * Fired when a friend was added
     * @param collar client
     * @param friendsApi friends
     * @param added added
     */
    default void onFriendAdded(Collar collar, FriendsApi friendsApi, Friend added) {}

    /**
     * Fired when a friend was removed
     * @param collar client
     * @param friendsApi friends
     * @param removed added
     */
    default void onFriendRemoved(Collar collar, FriendsApi friendsApi, Friend removed) {}
}
