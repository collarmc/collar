package com.collarmc.client.api.friends.events;

import com.collarmc.api.friends.Friend;
import com.collarmc.client.Collar;
import com.collarmc.client.events.AbstractCollarEvent;

/**
 * Fired when a friend was added
 */
public final class FriendAddedEvent extends AbstractCollarEvent {
    public final Friend friend;

    public FriendAddedEvent(Collar collar, Friend friend) {
        super(collar);
        this.friend = friend;
    }
}
