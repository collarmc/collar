package com.collarmc.tests.server;

import com.collarmc.api.http.RequestContext;
import com.collarmc.api.profiles.Profile;
import com.collarmc.client.Collar;
import com.collarmc.client.CollarConfiguration;
import com.collarmc.server.configuration.Configuration;
import com.collarmc.server.mongo.Mongo;
import com.collarmc.server.security.mojang.NojangMinecraftSessionVerifier;
import com.collarmc.server.services.profiles.ProfileServiceServer;
import com.collarmc.tests.junit.CollarClientRule;
import com.collarmc.tests.junit.CollarServerRule;
import com.google.common.io.Files;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.collarmc.tests.junit.CollarAssert.waitForCondition;

public class SimpleTest {

    private final AtomicReference<Profile> aliceProfile = new AtomicReference<>();

    UUID alicePlayerId = UUID.randomUUID();

    private final File tempDir = Files.createTempDir();

    @Rule
    public CollarServerRule serverRule = new CollarServerRule(services -> {
        aliceProfile.set(services.profiles.createProfile(RequestContext.ANON, new ProfileServiceServer.CreateProfileRequest(
                "alice@example.com",
                "alice",
                "Alice UwU"
        )).profile);
    }, Configuration.testConfiguration(Mongo.getTestingDatabase(), new NojangMinecraftSessionVerifier()));

    @Rule
    public CollarClientRule alicePlayer = new CollarClientRule(aliceProfile, alicePlayerId, serverRule, new CollarConfiguration.Builder()
            .withHomeDirectory(tempDir)
            .withEntitiesSupplier(Set::of)
    );

    @Test
    public void singleClientConnectsAndDisconnects() {
        // Wait for the server to start and for alice to connect
        waitForCondition("server started", () -> serverRule.isServerStarted());
        waitForCondition("client connected", () -> alicePlayer.collar.getState() == Collar.State.CONNECTED);
        // Disconnect alice
        alicePlayer.collar.disconnect();
        waitForCondition("client disconnected", () -> alicePlayer.collar.getState() == Collar.State.DISCONNECTED);
        // Reconnect alice
        alicePlayer.collar.connect();
        waitForCondition("client connected", () -> alicePlayer.collar.getState() == Collar.State.CONNECTED);
    }
}
