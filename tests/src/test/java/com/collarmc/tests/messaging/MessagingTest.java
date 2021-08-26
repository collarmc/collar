package com.collarmc.tests.messaging;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.messaging.Message;
import com.collarmc.api.messaging.TextMessage;
import com.collarmc.api.session.Player;
import com.collarmc.client.Collar;
import com.collarmc.client.api.messaging.MessagingApi;
import com.collarmc.client.api.messaging.MessagingListener;
import com.collarmc.security.mojang.MinecraftPlayer;
import com.collarmc.tests.junit.CollarTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

import static com.collarmc.tests.junit.CollarAssert.waitForCondition;

public class MessagingTest extends CollarTest {
    @Test
    public void aliceSendsBobAnUwU() {
        MessagingListenerImpl bobMessageListener = new MessagingListenerImpl();
        MessagingListenerImpl aliceListener = new MessagingListenerImpl();
        bobPlayer.collar.messaging().subscribe(bobMessageListener);
        alicePlayer.collar.messaging().subscribe(aliceListener);
        alicePlayer.collar.messaging().sendPrivateMessage(bobPlayer.collar.player(), new TextMessage("UwU"));
        waitForCondition("Alice sent an UwU to Bob", () -> {
            if (!(bobMessageListener.lastMessage instanceof TextMessage)) {
                return false;
            }
            TextMessage message = (TextMessage) bobMessageListener.lastMessage;
            return message.content.equals("UwU");
        });
        Assert.assertEquals("UwU", ((TextMessage)aliceListener.lastMessageSent).content);
    }

    @Test
    public void aliceSendsNonCollarPlayerAnUwU() {
        MessagingListenerImpl aliceListener = new MessagingListenerImpl();
        alicePlayer.collar.messaging().subscribe(aliceListener);
        alicePlayer.collar.messaging().sendPrivateMessage(new Player(new ClientIdentity(UUID.randomUUID(), null), new MinecraftPlayer(UUID.randomUUID(), "hypixel.net", 1)), new TextMessage("UwU"));
        waitForCondition("Alice could not send private message", () -> {
            if (!(aliceListener.lastUntrustedMessage instanceof TextMessage)) {
                return false;
            }
            TextMessage message = (TextMessage) aliceListener.lastUntrustedMessage;
            return message.content.equals("UwU");
        });
    }

    private static final class MessagingListenerImpl implements MessagingListener {
        Message lastMessage;
        Message lastUntrustedMessage;
        Message lastMessageSent;

        @Override
        public void onPrivateMessageReceived(Collar collar, MessagingApi messagingApi, Player sender, Message message) {
            this.lastMessage = message;
        }

        @Override
        public void onPrivateMessageRecipientIsUntrusted(Collar collar, MessagingApi messagingApi, MinecraftPlayer player, Message message) {
            this.lastUntrustedMessage = message;
        }

        @Override
        public void onPrivateMessageSent(Collar collar, MessagingApi messagingApi, Player player, Message message) {
            this.lastMessageSent = message;
        }
    }
}
