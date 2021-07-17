package com.collarmc.client.api.messaging;

import com.collarmc.api.groups.Group;
import com.collarmc.api.messaging.Message;
import com.collarmc.api.session.Player;
import com.collarmc.client.Collar;
import com.collarmc.client.api.ApiListener;
import com.collarmc.security.mojang.MinecraftPlayer;

public interface MessagingListener extends ApiListener {
    /**
     * Fired when a private message was sent to another player via collar
     * @param collar client
     * @param messagingApi api
     * @param player sent to
     * @param message the message
     */
    default void onPrivateMessageSent(Collar collar, MessagingApi messagingApi, Player player, Message message) {}

    /**
     * Fired when a private message was attempted with another player but there was not sufficent trust to deliver
     * e.g. player was using a non-collar enabled client
     * In this case, you should warn the user and send the Minecraft server protocol message
     * @param collar client
     * @param messagingApi api
     * @param player we sent the message to
     * @param message the message
     */
    default void onPrivateMessageRecipientIsUntrusted(Collar collar, MessagingApi messagingApi, MinecraftPlayer player, Message message) {}

    /**
     * Fired when a private message was received from another collar player
     * @param collar client
     * @param messagingApi api
     * @param sender who sent it
     * @param message the message
     */
    default void onPrivateMessageReceived(Collar collar, MessagingApi messagingApi, Player sender, Message message) {}

    /**
     * Fired when a group message was sent to another player via collar
     * @param collar client
     * @param messagingApi api
     * @param group sent to
     * @param message the message
     */
    default void onGroupMessageSent(Collar collar, MessagingApi messagingApi, Group group, Message message) {}

    /**
     * Fired when a group message was received
     * @param collar client
     * @param messagingApi api
     * @param group sent to
     * @param sender who sent it
     * @param message the message
     */
    default void onGroupMessageReceived(Collar collar, MessagingApi messagingApi, Group group, Player sender, Message message) {}
}
