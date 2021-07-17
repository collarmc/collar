package com.collarmc.tests.server;

import com.collarmc.tests.junit.CollarServerRule;
import com.google.common.io.Files;
import org.junit.Rule;
import org.junit.Test;
import com.collarmc.api.http.RequestContext;
import com.collarmc.api.profiles.Profile;
import com.collarmc.client.Collar;
import com.collarmc.client.CollarConfiguration;
import com.collarmc.server.Services;
import com.collarmc.server.configuration.Configuration;
import com.collarmc.server.mongo.Mongo;
import com.collarmc.server.security.mojang.NojangMinecraftSessionVerifier;
import com.collarmc.server.services.profiles.ProfileServiceServer;
import com.collarmc.tests.junit.CollarClientRule;
import com.collarmc.tests.junit.CollarTest;

import java.io.File;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.collarmc.tests.junit.CollarAssert.waitForCondition;

public class HandshakeTest {

    private final AtomicReference<Services> services = new AtomicReference<>();
    private final AtomicReference<Profile> aliceProfile = new AtomicReference<>();
    private final AtomicInteger devicesConfirmed = new AtomicInteger(0);
    UUID alicePlayerId = UUID.randomUUID();

    private final File tempDir = Files.createTempDir();

    @Rule
    public CollarServerRule serverRule = new CollarServerRule(services -> {
        this.services.set(services);
        aliceProfile.set(services.profiles.createProfile(RequestContext.ANON, new ProfileServiceServer.CreateProfileRequest(
                "alice@example.com",
                "alice",
                "Alice UwU"
        )).profile);
    }, Configuration.testConfiguration(Mongo.getTestingDatabase(), new NojangMinecraftSessionVerifier()));

    @Rule
    public CollarClientRule alicePlayer = new CollarClientRule(alicePlayerId, new CollarConfiguration.Builder()
            .withListener(new CollarTest.ApprovingListener(aliceProfile, services, devicesConfirmed))
            .withHomeDirectory(tempDir)
            .withEntitiesSupplier(Set::of)
    );

    @Test
    public void initialRegistrationAndLoginDoesNotExplode() throws Exception {
        // Wait for the server to start and for alice to connect
        waitForCondition("server started", () -> serverRule.isServerStarted());
        waitForCondition("device registered", () -> devicesConfirmed.get() == 1);
        waitForCondition("client connected", () -> alicePlayer.collar.getState() == Collar.State.CONNECTED);

        // Disconnect alice
        alicePlayer.collar.disconnect();

        waitForCondition("client disconnected", () -> alicePlayer.collar.getState() == Collar.State.DISCONNECTED);

        devicesConfirmed.set(0);

        alicePlayer.collar.connect();

        waitForCondition("client connected", () -> alicePlayer.collar.getState() == Collar.State.CONNECTED);
        waitForCondition("device registered", () -> devicesConfirmed.get() == 0);
    }
}
