package com.collarmc.client.api.messaging.events;

import com.collarmc.api.groups.Group;
import com.collarmc.api.messaging.Message;
import com.collarmc.api.session.Player;
import com.collarmc.client.Collar;
import com.collarmc.client.events.AbstractCollarEvent;

/**
 * Fired when a group message was received
 */
public final class GroupMessageReceivedEvent extends AbstractCollarEvent {
    public final Group group;
    public final Player sender;
    public final Message message;

    public GroupMessageReceivedEvent(Collar collar, Group group, Player sender, Message message) {
        super(collar);
        this.group = group;
        this.sender = sender;
        this.message = message;
    }
}
