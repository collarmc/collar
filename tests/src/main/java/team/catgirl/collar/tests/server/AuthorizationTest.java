package team.catgirl.collar.tests.server;

import org.junit.Test;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.tests.junit.CollarTest;

import static team.catgirl.collar.tests.junit.CollarAssert.waitForCondition;

public class AuthorizationTest extends CollarTest {
    @Test
    public void connect() throws InterruptedException {
        waitForCondition("client connected", () -> aliceUser.collar.getState() == Collar.State.CONNECTED);
        waitForCondition("client connected", () -> bobUser.collar.getState() == Collar.State.CONNECTED);
        waitForCondition("client connected", () -> eveUser.collar.getState() == Collar.State.CONNECTED);
    }
}
