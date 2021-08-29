package com.collarmc.client.api.friends.events;

import com.collarmc.api.friends.Friend;
import com.collarmc.client.Collar;
import com.collarmc.client.events.AbstractCollarEvent;

/**
 * Fired when the local state of a friend has changed (e.g. online, offline, loading friends list)
 */
public final class FriendChangedEvent extends AbstractCollarEvent {
    public final Friend friend;

    public FriendChangedEvent(Collar collar, Friend friend) {
        super(collar);
        this.friend = friend;
    }
}
