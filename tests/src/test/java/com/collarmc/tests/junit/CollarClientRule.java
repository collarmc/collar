package com.collarmc.tests.junit;

import com.collarmc.security.mojang.MinecraftSession;
import com.google.common.io.Files;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import com.collarmc.client.Collar;
import com.collarmc.client.CollarConfiguration;
import com.collarmc.client.minecraft.Ticks;

import java.util.UUID;

public final class CollarClientRule implements TestRule {

    public Collar collar;
    private Ticks ticks;

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

    public CollarClientRule(UUID playerId, CollarConfiguration.Builder builder) {
        this(playerId, builder, MinecraftSession.noJang(playerId, "cuteplayer", 0, "hypixel.net"));
    }
    public CollarClientRule(UUID playerId, CollarConfiguration.Builder builder, MinecraftSession session) {
        this.ticks = new Ticks();
        this.builder = builder.withCollarServer("http://localhost:3001")
                .withHomeDirectory(Files.createTempDir())
                .withSession(() -> session)
                .withTicks(ticks);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                collar = Collar.create(builder.build());
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
