package team.catgirl.collar.api.friends;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FriendList {
    private final Set<UUID> friends;

    public FriendList(Set<UUID> friends) {
        this.friends = friends;
    }

    public FriendList addFriend(UUID friend) {
        Set<UUID> friends = new HashSet<>();
        friends.addAll(friends);
        friends.add(friend);
        return new FriendList(friends);
    }
}
