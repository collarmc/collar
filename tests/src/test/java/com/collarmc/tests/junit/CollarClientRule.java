package com.collarmc.tests.junit;

import com.collarmc.api.profiles.Profile;
import com.collarmc.client.Collar;
import com.collarmc.client.CollarConfiguration;
import com.collarmc.client.minecraft.Ticks;
import com.collarmc.pounce.EventBus;
import com.collarmc.security.mojang.MinecraftSession;
import com.google.common.io.Files;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public final class CollarClientRule implements TestRule {

    private final CollarServerRule serverRule;
    private final AtomicReference<Profile> profile;
    public Collar collar;
    public EventBus eventBus;
    private Ticks ticks;
    private ApprovingListener approvingListener;

    private final CollarConfiguration.Builder builder;
    private final Thread thread = new Thread(() -> {
        collar.connect();
        do {
            try {
                if (collar.getState() == Collar.State.CONNECTED) {
                    ticks.onTick();
                }
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
                collar.disconnect();
            }
        } while (collar.getState() != Collar.State.DISCONNECTED);
    }, "Collar Client Test Loop");

    public CollarClientRule(AtomicReference<Profile> profile, UUID playerId, CollarServerRule serverRule, CollarConfiguration.Builder builder) {
        this(profile, builder, serverRule, MinecraftSession.noJang(playerId, "cuteplayer", 0, "hypixel.net"));
    }

    public CollarClientRule(AtomicReference<Profile> profile, CollarConfiguration.Builder builder, CollarServerRule serverRule, MinecraftSession session) {
        this.profile = profile;
        this.serverRule = serverRule;
        this.ticks = new Ticks();
        this.builder = builder.withCollarServer("http://localhost:3001")
                .withHomeDirectory(Files.createTempDir())
                .withSession(() -> session)
                .withTicks(ticks)
                .withEventBus(eventBus);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                eventBus = new EventBus(Runnable::run);
                approvingListener = new ApprovingListener(profile.get().id, serverRule.services, eventBus);
                collar = Collar.create(builder.withEventBus(eventBus).build());
                thread.start();
                try {
                    base.evaluate();
                } finally {
                    thread.interrupt();
                }
            }
        };
    }
}
