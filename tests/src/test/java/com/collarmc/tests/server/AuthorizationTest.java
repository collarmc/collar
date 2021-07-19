package com.collarmc.tests.server;

import org.junit.Test;
import com.collarmc.client.Collar;
import com.collarmc.tests.junit.CollarTest;

import static com.collarmc.tests.junit.CollarAssert.waitForCondition;

public class AuthorizationTest extends CollarTest {
    @Test
    public void connect() {
        waitForCondition("client connected", () -> alicePlayer.collar.getState() == Collar.State.CONNECTED);
        waitForCondition("client connected", () -> bobPlayer.collar.getState() == Collar.State.CONNECTED);
        waitForCondition("client connected", () -> evePlayer.collar.getState() == Collar.State.CONNECTED);
    }
}