package team.catgirl.collar.tests.server;

import org.junit.Rule;
import org.junit.Test;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.CollarConfiguration;
import team.catgirl.collar.client.CollarListener;
import team.catgirl.collar.server.Services;
import team.catgirl.collar.server.http.RequestContext;
import team.catgirl.collar.server.services.devices.Device;
import team.catgirl.collar.server.services.devices.DeviceService.CreateDeviceResponse;
import team.catgirl.collar.server.services.profiles.Profile;
import team.catgirl.collar.server.services.profiles.ProfileService;
import team.catgirl.collar.tests.junit.CollarClientRule;
import team.catgirl.collar.tests.junit.CollarServerRule;

import static team.catgirl.collar.tests.junit.CollarAssert.waitForCondition;

public class AuthorizationTest {

    CollarListener listener = new CollarListener() {
        @Override
        public void onConfirmDeviceRegistration(Collar collar, String token, String approvalUrl) {
            // We can probably safely assume that this is device 1 for testing purposes
            CreateDeviceResponse resp = new CreateDeviceResponse(new Device(aliceProfile.id, 1, "Cool Computer Beep Boop"));
            services.sessions.onDeviceRegistered(services.identityStore.getIdentity(), aliceProfile.toPublic(), token, resp);
            deviceConfirmed = true;
        }
    };

    protected Services services;
    protected Profile aliceProfile;

    Boolean deviceConfirmed = false;

    @Rule
    public CollarServerRule serverRule = new CollarServerRule(services -> {
        this.services = services;
        String username = "user" + System.currentTimeMillis();
        aliceProfile = services.profiles.createProfile(RequestContext.ANON, new ProfileService.CreateProfileRequest(
                username + "@example.com",
                username,
                "nice profile"
        )).profile;
    });

    @Rule
    public CollarClientRule aliceUser = new CollarClientRule(new CollarConfiguration.Builder().withListener(listener));

    @Test
    public void startStopServer() throws InterruptedException {
        waitForCondition("server started", () -> serverRule.isServerStarted());
        waitForCondition("device registered", () -> deviceConfirmed);
        waitForCondition("client connected", () -> aliceUser.collar.getState() == Collar.State.CONNECTED);
    }
}
