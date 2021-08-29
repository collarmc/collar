package com.collarmc.client.api.messaging.events;

import com.collarmc.api.messaging.Message;
import com.collarmc.api.session.Player;
import com.collarmc.client.Collar;
import com.collarmc.client.events.AbstractCollarEvent;

/**
 * Fired when a private message was received from another collar player
 */
public final class PrivateMessageReceivedEvent extends AbstractCollarEvent {
    public final Player player;
    public final Message message;

    public PrivateMessageReceivedEvent(Collar collar, Player player, Message message) {
        super(collar);
        this.player = player;
        this.message = message;
    }
}
