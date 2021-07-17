package com.collarmc.client.api.messaging;

import com.collarmc.client.api.AbstractApi;
import com.collarmc.client.security.ClientIdentityStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.collarmc.api.groups.Group;
import com.collarmc.api.messaging.Message;
import com.collarmc.api.session.Player;
import com.collarmc.client.Collar;
import com.collarmc.client.api.identity.IdentityApi;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.protocol.messaging.SendMessageRequest;
import com.collarmc.protocol.messaging.SendMessageResponse;
import com.collarmc.security.cipher.Cipher;
import com.collarmc.security.cipher.CipherException;
import com.collarmc.utils.Utils;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MessagingApi extends AbstractApi<MessagingListener> {

    private static final Logger LOGGER = LogManager.getLogger(MessagingApi.class.getName());

    public MessagingApi(Collar collar, Supplier<ClientIdentityStore> identityStoreSupplier, Consumer<ProtocolRequest> sender) {
        super(collar, identityStoreSupplier, sender);
    }

    /**
     * Sends a private message to another player
     * @param player to send the message to
     * @param message the player
     */
    public void sendPrivateMessage(Player player, Message message) {
        IdentityApi identityApi = collar.identities();
        identityApi.identify(player.minecraftPlayer.id)
                .thenCompose(identityApi::createTrust)
                .thenAccept(sender -> {
                    if (sender.isPresent()) {
                        Cipher cipher = identityStore().createCypher();
                        byte[] messageBytes;
                        try {
                            messageBytes = cipher.crypt(sender.get(), Utils.messagePackMapper().writeValueAsBytes(message));
                        } catch (JsonProcessingException | CipherException e) {
                            throw new IllegalStateException(collar.identity() + " could not process private message from " + sender, e);
                        }
                        this.sender.accept(new SendMessageRequest(collar.identity(), sender.get(), null, messageBytes));
                        fireListener("onPrivateMessageSent", listener -> {
                            listener.onPrivateMessageSent(collar, this, player, message);
                        });
                    } else {
                        LOGGER.info(collar.identity() + " could not locate identity for " + player + ". The private message was not sent.");
                        fireListener("onPrivateMessageRecipientIsUntrusted", listener -> {
                            listener.onPrivateMessageRecipientIsUntrusted(collar, this, player.minecraftPlayer, message);
                        });
                    }
                });
    }

    /**
     * Sends a message to the specified group
     * @param group to send to
     * @param message to send
     */
    public void sendGroupMessage(Group group, Message message) {
        LOGGER.info(identity() + " sending message to group " + group.id);
        Cipher cipher = identityStore().createCypher();
        byte[] messageBytes;
        try {
            messageBytes = cipher.crypt(identity(), group, Utils.messagePackMapper().writeValueAsBytes(message));
        } catch (Throwable e) {
            // If the client cant send a message to the group, something is seriously wrong
            throw new IllegalStateException(collar.identity() + " could not encrypt group message sent to " + group.id, e);
        }
        sender.accept(new SendMessageRequest(collar.identity(), null, group.id, messageBytes));
        LOGGER.info(identity() + " sent message to group " + group.id);
        fireListener("onGroupMessageSent", listener -> {
            listener.onGroupMessageSent(collar, this, group, message);
        });
    }

    /**
     * Send a message to players nearby
     * @param message to send
     */
    public void sendNearbyMessage(Message message) {
        collar.groups().nearby().forEach(group -> sendGroupMessage(group, message));
    }

    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        if (resp instanceof SendMessageResponse) {
            SendMessageResponse response = (SendMessageResponse) resp;
            if (response.group != null && response.sender != null) {
                collar.groups().findGroupById(response.group).ifPresent(group -> {
                    Message message;
                    try {
                        byte[] decryptedBytes = identityStore().createCypher().decrypt(response.sender, group, response.message);
                        message = Utils.messagePackMapper().readValue(decryptedBytes, Message.class);
                    } catch (IOException | CipherException e) {
                        // We don't throw an exception here in case someone is doing something naughty to disrupt the group and cause the client to exit
                        LOGGER.error(collar.identity() + "could not read group message from group " + group.id, e);
                        message = null;
                    }
                    if (message != null) {
                        Message finalMessage = message;
                        fireListener("onGroupMessageReceived", listener -> {
                            listener.onGroupMessageReceived(collar, this, group, response.player, finalMessage);
                        });
                    }
                });
            } else if (response.sender != null) {
                Message message;
                try {
                    byte[] decryptedBytes = identityStore().createCypher().decrypt(response.sender, response.message);
                    message = Utils.messagePackMapper().readValue(decryptedBytes, Message.class);
                } catch (IOException | CipherException e) {
                    throw new IllegalStateException(collar.identity() + "Could not read private message from " + response.sender, e);
                }
                fireListener("onPrivateMessageReceived", listener -> {
                    listener.onPrivateMessageReceived(collar, this, response.player, message);
                });
            } else {
                LOGGER.warn( collar.identity() + "could not process message. It was not addressed correctly.");
            }
            return true;
        }
        return false;
    }

    @Override
    public void onStateChanged(Collar.State state) {}
}
