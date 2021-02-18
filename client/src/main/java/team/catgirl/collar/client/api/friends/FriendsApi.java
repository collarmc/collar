package team.catgirl.collar.client.api.friends;

import team.catgirl.collar.api.friends.Friend;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.Collar.State;
import team.catgirl.collar.client.api.AbstractApi;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.friends.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FriendsApi extends AbstractApi<FriendsListener> {

    private final ConcurrentMap<UUID, Friend> friends = new ConcurrentHashMap<>();

    public FriendsApi(Collar collar, Supplier<ClientIdentityStore> identityStoreSupplier, Consumer<ProtocolRequest> sender) {
        super(collar, identityStoreSupplier, sender);
    }

    /**
     * @return friends of this player
     */
    public Set<Friend> list() {
        return new HashSet<>(friends.values());
    }

    /**
     * Add a friend by their player id
     * @param player id
     */
    public void addFriendByPlayerId(UUID player) {
        sender.accept(new AddFriendRequest(identity(), player, null));
    }

    /**
     * Add a friend by their collar profile id
     * @param profile id
     */
    public void addFriendByProfileId(UUID profile) {
        sender.accept(new AddFriendRequest(identity(), null, profile));
    }

    /**
     * Remove a friend by their player id
     * @param player id
     */
    public void removeFriendByPlayerId(UUID player) {
        sender.accept(new RemoveFriendRequest(identity(), player, null));
    }

    /**
     * Remove a friend by their collar profile id
     * @param profile id
     */
    public void removeFriendByProfileId(UUID profile) {
        sender.accept(new RemoveFriendRequest(identity(), null, profile));
    }

    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        if (resp instanceof GetFriendListResponse) {
            GetFriendListResponse response = (GetFriendListResponse) resp;
            response.friends.forEach(friend -> {
                friends.put(friend.friend, friend);
                fireListener("onFriendStatusChanged", listener -> {
                    listener.onFriendChanged(collar, this, friend);
                });
            });
            return true;
        } else if (resp instanceof FriendChangedResponse) {
            FriendChangedResponse response = (FriendChangedResponse) resp;
            friends.put(response.friend.friend, response.friend);
            fireListener("onFriendChanged", listener -> {
                listener.onFriendChanged(collar, this, response.friend);
            });
            return true;
        } else if (resp instanceof AddFriendResponse) {
            AddFriendResponse response = (AddFriendResponse) resp;
            friends.put(response.friend.friend, response.friend);
            fireListener("onFriendAdded", listener -> {
                listener.onFriendAdded(collar, this, response.friend);
            });
            return true;
        } else if (resp instanceof RemoveFriendResponse) {
            RemoveFriendResponse response = (RemoveFriendResponse) resp;
            Friend removed = friends.remove(response.friend);
            if (removed != null) {
                fireListener("onFriendRemoved", listener -> {
                    listener.onFriendRemoved(collar, this, removed);
                });
            }
            return true;
        }
        return false;
    }

    @Override
    public void onStateChanged(State state) {
        switch (state) {
            case CONNECTED:
                sender.accept(new GetFriendListRequest(identity()));
                break;
            case DISCONNECTED:
                friends.clear();
                break;
        }
    }
}
