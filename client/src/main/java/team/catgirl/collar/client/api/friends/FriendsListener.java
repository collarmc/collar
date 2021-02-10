package team.catgirl.collar.client.api.friends;

import team.catgirl.collar.api.friends.Friend;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.ApiListener;

public interface FriendsListener extends ApiListener {
    default void onFriendChanged(Collar collar, FriendsApi friendsApi, Friend friend) {};
    default void onFriendAdded(Collar collar, FriendsApi friendsApi, Friend friend) {};
    default void onFriendRemoved(Collar collar, FriendsApi friendsApi, Friend removed) {};
}
