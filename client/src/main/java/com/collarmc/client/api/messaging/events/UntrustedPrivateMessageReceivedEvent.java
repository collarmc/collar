package com.collarmc.client.api.messaging.events;

import com.collarmc.api.messaging.Message;
import com.collarmc.client.Collar;
import com.collarmc.client.events.AbstractCollarEvent;
import com.collarmc.api.minecraft.MinecraftPlayer;

/**
 * Fired when a private message was attempted with another player but there was not sufficient trust to deliver
 */
public final class UntrustedPrivateMessageReceivedEvent extends AbstractCollarEvent {
    public final MinecraftPlayer player;
    public final Message message;

    public UntrustedPrivateMessageReceivedEvent(Collar collar, MinecraftPlayer player, Message message) {
        super(collar);
        this.player = player;
        this.message = message;
    }
}
