package com.collarmc.tests.messaging;

import com.collarmc.api.entities.Entity;
import com.collarmc.api.entities.EntityType;
import com.collarmc.api.groups.Group;
import com.collarmc.api.messaging.TextMessage;
import com.collarmc.tests.groups.GroupsTest.MessagingListenerImpl;
import com.collarmc.tests.junit.CollarTest;
import com.collarmc.utils.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

import static com.collarmc.tests.junit.CollarAssert.waitForCondition;

public class NearbyMessagingTest extends CollarTest {

    int aliceEntityId = Utils.secureRandom().nextInt();
    int bobEntityId = Utils.secureRandom().nextInt();
    int eveEntityId = Utils.secureRandom().nextInt();

    @Test
    public void sendNearbyMessage() {
        MessagingListenerImpl aliceMessages = new MessagingListenerImpl(alicePlayer.eventBus);
        MessagingListenerImpl bobMessages = new MessagingListenerImpl(bobPlayer.eventBus);
        MessagingListenerImpl eveMessages = new MessagingListenerImpl(evePlayer.eventBus);

        waitForCondition("Alice joined nearby group", () -> !alicePlayer.collar.groups().nearby().isEmpty());
        waitForCondition("Bob joined nearby group", () -> !bobPlayer.collar.groups().nearby().isEmpty());

        Group aliceGroup = alicePlayer.collar.groups().nearby().get(0);
        Group bobGroup = bobPlayer.collar.groups().nearby().get(0);

        Assert.assertEquals(aliceGroup.id, bobGroup.id);

        bobPlayer.collar.messaging().sendNearbyMessage(new TextMessage("Marco?"));
        alicePlayer.collar.messaging().sendNearbyMessage(new TextMessage("Polo!"));

        waitForCondition("alice receives Marco", () -> aliceMessages.lastReceivedMessage instanceof TextMessage && "Marco?".equals(((TextMessage) aliceMessages.lastReceivedMessage).content));
        waitForCondition("bob receives Polo", () -> bobMessages.lastReceivedMessage instanceof TextMessage && "Polo!".equals(((TextMessage) bobMessages.lastReceivedMessage).content));
        Assert.assertNull("eve received no messages", eveMessages.lastReceivedMessage);
    }

    @Override
    protected Set<Entity> aliceEntities() {
        return Set.of(new Entity(bobEntityId, EntityType.PLAYER), new Entity(aliceEntityId, EntityType.PLAYER));
    }

    @Override
    protected Set<Entity> bobEntities() {
        return Set.of(new Entity(aliceEntityId, EntityType.PLAYER), new Entity(bobEntityId, EntityType.PLAYER));
    }

    @Override
    protected Set<Entity> eveEntities() {
        // Eve is evil and is trying to trick us into thinking she's in proximity to Alice
        return Set.of(new Entity(aliceEntityId, EntityType.PLAYER), new Entity(eveEntityId, EntityType.PLAYER));
    }
}
