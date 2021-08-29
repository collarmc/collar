package com.collarmc.client.api.friends;

import com.collarmc.api.friends.Friend;
import com.collarmc.client.Collar;
import com.collarmc.client.Collar.State;
import com.collarmc.client.api.AbstractApi;
import com.collarmc.client.api.friends.events.FriendAddedEvent;
import com.collarmc.client.api.friends.events.FriendChangedEvent;
import com.collarmc.client.api.friends.events.FriendRemovedEvent;
import com.collarmc.client.security.ClientIdentityStore;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.protocol.friends.*;
import com.collarmc.security.mojang.MinecraftPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FriendsApi extends AbstractApi {

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
     * @param player to add
     */
    public void addFriend(MinecraftPlayer player) {
        sender.accept(new AddFriendRequest(player.id, null));
    }

    /**
     * Add a friend by their collar profile id
     * @param profile id
     */
    public void addFriend(UUID profile) {
        sender.accept(new AddFriendRequest(null, profile));
    }

    /**
     * Remove a friend by their player id
     * @param player to remove
     */
    public void removeFriend(MinecraftPlayer player) {
        sender.accept(new RemoveFriendRequest(player.id, null));
    }

    /**
     * Remove a friend by their collar profile id
     * @param profile id
     */
    public void removeFriend(UUID profile) {
        sender.accept(new RemoveFriendRequest(null, profile));
    }

    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        if (resp instanceof GetFriendListResponse) {
            GetFriendListResponse response = (GetFriendListResponse) resp;
            response.friends.forEach(friend -> {
                friends.put(friend.friend.id, friend);
                collar.configuration.eventBus.dispatch(new FriendChangedEvent(collar, friend));
            });
            return true;
        } else if (resp instanceof FriendChangedResponse) {
            FriendChangedResponse response = (FriendChangedResponse) resp;
            friends.put(response.friend.friend.id, response.friend);
            collar.configuration.eventBus.dispatch(new FriendChangedEvent(collar, response.friend));
            return true;
        } else if (resp instanceof AddFriendResponse) {
            AddFriendResponse response = (AddFriendResponse) resp;
            friends.put(response.friend.friend.id, response.friend);
            collar.configuration.eventBus.dispatch(new FriendAddedEvent(collar, response.friend));
            return true;
        } else if (resp instanceof RemoveFriendResponse) {
            RemoveFriendResponse response = (RemoveFriendResponse) resp;
            Friend removed = friends.remove(response.friend);
            if (removed != null) {
                collar.configuration.eventBus.dispatch(new FriendRemovedEvent(removed));
            }
            return true;
        }
        return false;
    }

    @Override
    public void onStateChanged(State state) {
        switch (state) {
            case CONNECTED:
                sender.accept(new GetFriendListRequest());
                break;
            case DISCONNECTED:
                friends.clear();
                break;
            default:
                break;
        }
    }
}
