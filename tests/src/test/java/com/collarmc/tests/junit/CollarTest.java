package com.collarmc.tests.junit;

import com.collarmc.api.entities.Entity;
import com.collarmc.api.http.RequestContext;
import com.collarmc.api.location.Dimension;
import com.collarmc.api.location.Location;
import com.collarmc.api.profiles.Profile;
import com.collarmc.client.Collar;
import com.collarmc.client.CollarConfiguration;
import com.collarmc.server.Services;
import com.collarmc.server.configuration.Configuration;
import com.collarmc.server.mongo.Mongo;
import com.collarmc.server.security.mojang.NojangMinecraftSessionVerifier;
import com.collarmc.server.services.profiles.ProfileServiceServer;
import com.collarmc.utils.Utils;
import org.junit.Before;
import org.junit.Rule;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public abstract class CollarTest {

    private final AtomicReference<Services> services = new AtomicReference<>();
    protected final AtomicReference<Profile> aliceProfile = new AtomicReference<>();
    protected final AtomicReference<Profile> bobProfile = new AtomicReference<>();
    protected final AtomicReference<Profile> eveProfile = new AtomicReference<>();
    protected final UUID alicePlayerId = UUID.randomUUID();
    protected final UUID bobPlayerId = UUID.randomUUID();
    protected final UUID evePlayerId = UUID.randomUUID();


    protected Services getServices() {
        return services.get();
    }

    protected void withServices(Services services) {}

    @Rule
    public CollarServerRule serverRule = new CollarServerRule(services -> {
        this.services.set(services);
         aliceProfile.set(services.profiles.createProfile(RequestContext.ANON, new ProfileServiceServer.CreateProfileRequest(
                "alice@example.com",
                "alice",
                "Alice"
        )).profile);
        bobProfile.set(services.profiles.createProfile(RequestContext.ANON, new ProfileServiceServer.CreateProfileRequest(
                "bob@example.com",
                "bob",
                "Bob"
        )).profile);
        eveProfile.set(services.profiles.createProfile(RequestContext.ANON, new ProfileServiceServer.CreateProfileRequest(
                "eve@example.com",
                "eve",
                "Eve"
        )).profile);

        withServices(services);
    }, Configuration.testConfiguration(Mongo.getTestingDatabase(), new NojangMinecraftSessionVerifier()));

    // Generates a new location on every call
    private final Supplier<Location> locationSupplier = () -> new Location(Utils.secureRandom().nextDouble(), Utils.secureRandom().nextDouble(), Utils.secureRandom().nextDouble(), Dimension.OVERWORLD);

    @Rule
    public CollarClientRule alicePlayer = new CollarClientRule(aliceProfile, alicePlayerId, serverRule, new CollarConfiguration.Builder()
                .withPlayerLocation(locationSupplier)
                .withEntitiesSupplier(this::aliceEntities)
    );

    @Rule
    public CollarClientRule bobPlayer = new CollarClientRule(bobProfile, bobPlayerId, serverRule, new CollarConfiguration.Builder()
            .withPlayerLocation(locationSupplier)
            .withEntitiesSupplier(this::bobEntities)
    );

    @Rule
    public CollarClientRule evePlayer = new CollarClientRule(eveProfile, evePlayerId, serverRule, new CollarConfiguration.Builder()
            .withPlayerLocation(locationSupplier)
            .withEntitiesSupplier(this::eveEntities)
    );

    protected Set<Entity> aliceEntities() {
        return Set.of();
    }

    protected Set<Entity> bobEntities() {
        return Set.of();
    }

    protected Set<Entity> eveEntities() {
        return Set.of();
    }

    @Before
    public void waitForClientsToConnect() {
        CollarAssert.waitForCondition("server started", () -> serverRule.isServerStarted());
        CollarAssert.waitForCondition("alice connected", () -> alicePlayer.collar.getState() == Collar.State.CONNECTED, 25, TimeUnit.SECONDS);
        CollarAssert.waitForCondition("bob connected", () -> bobPlayer.collar.getState() == Collar.State.CONNECTED, 25, TimeUnit.SECONDS);
        CollarAssert.waitForCondition("eve connected", () -> evePlayer.collar.getState() == Collar.State.CONNECTED, 25, TimeUnit.SECONDS);

        System.out.println("Alice is identity " + alicePlayer.collar.identity());
        System.out.println("Bob is identity " + bobPlayer.collar.identity());
        System.out.println("Eve is identity " + evePlayer.collar.identity());

        System.out.println("Alice is player " + alicePlayer.collar.player());
        System.out.println("Bob is player " + bobPlayer.collar.player());
        System.out.println("Eve is player " + evePlayer.collar.player());
    }

}
