package com.collarmc.client.api.groups.events;

import com.collarmc.api.groups.Group;
import com.collarmc.api.session.Player;
import com.collarmc.client.Collar;
import com.collarmc.client.events.AbstractCollarEvent;

/**
 * Fired when a group is left by a player
 */
public final class GroupLeftEvent extends AbstractCollarEvent {
    public final Group group;
    public final Player player;

    public GroupLeftEvent(Collar collar, Group group, Player player) {
        super(collar);
        this.group = group;
        this.player = player;
    }
}
