package com.collarmc.tests.messaging;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.messaging.Message;
import com.collarmc.api.messaging.TextMessage;
import com.collarmc.api.session.Player;
import com.collarmc.client.api.messaging.events.PrivateMessageReceivedEvent;
import com.collarmc.client.api.messaging.events.PrivateMessageSentEvent;
import com.collarmc.client.api.messaging.events.UntrustedPrivateMessageReceivedEvent;
import com.collarmc.pounce.EventBus;
import com.collarmc.pounce.Subscribe;
import com.collarmc.api.minecraft.MinecraftPlayer;
import com.collarmc.tests.junit.CollarTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

import static com.collarmc.tests.junit.CollarAssert.waitForCondition;

public class MessagingTest extends CollarTest {
    @Test
    public void aliceSendsBobAnUwU() {
        MessagingListenerImpl bobMessageListener = new MessagingListenerImpl(bobPlayer.eventBus);
        MessagingListenerImpl aliceListener = new MessagingListenerImpl(alicePlayer.eventBus);
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
        MessagingListenerImpl aliceListener = new MessagingListenerImpl(alicePlayer.eventBus);
        alicePlayer.collar.messaging().sendPrivateMessage(new Player(new ClientIdentity(UUID.randomUUID(), null), new MinecraftPlayer(UUID.randomUUID(), "hypixel.net", 1)), new TextMessage("UwU"));
        waitForCondition("Alice could not send private message", () -> {
            if (!(aliceListener.lastUntrustedMessage instanceof TextMessage)) {
                return false;
            }
            TextMessage message = (TextMessage) aliceListener.lastUntrustedMessage;
            return message.content.equals("UwU");
        });
    }

    private static final class MessagingListenerImpl {
        Message lastMessage;
        Message lastUntrustedMessage;
        Message lastMessageSent;

        public MessagingListenerImpl(EventBus eventBus) {
            eventBus.subscribe(this);
        }

        @Subscribe
        public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
            this.lastMessage = event.message;
        }

        @Subscribe
        public void onPrivateMessageRecipientIsUntrusted(UntrustedPrivateMessageReceivedEvent event) {
            this.lastUntrustedMessage = event.message;
        }

        @Subscribe
        public void onPrivateMessageSent(PrivateMessageSentEvent event) {
            this.lastMessageSent = event.message;
        }
    }
}
