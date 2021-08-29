package com.collarmc.client.api.friends.events;

import com.collarmc.api.friends.Friend;

/**
 * Fired when a friend was removed
 */
public final class FriendRemovedEvent {
    public final Friend friend;

    public FriendRemovedEvent(Friend friend) {
        this.friend = friend;
    }
}
