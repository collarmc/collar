package com.collarmc.tests.server;

import com.collarmc.client.Collar;
import com.collarmc.tests.junit.CollarTest;
import org.junit.Test;

import static com.collarmc.tests.junit.CollarAssert.waitForCondition;

public class ConnectionTest extends CollarTest {
    @Test
    public void connectThreeClients() {
        waitForCondition("client connected", () -> alicePlayer.collar.getState() == Collar.State.CONNECTED);
        waitForCondition("client connected", () -> bobPlayer.collar.getState() == Collar.State.CONNECTED);
        waitForCondition("client connected", () -> evePlayer.collar.getState() == Collar.State.CONNECTED);
    }
}
