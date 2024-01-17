package com.collarmc.client.debug;

import com.collarmc.client.HomeDirectory;
import com.collarmc.security.mojang.MinecraftSession;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;

/**
 * Debugging tools for collar developers
 * To use place a file containing java properties at $minecraftHome/collar/debug
 */
public final class DebugConfiguration {
    public final boolean tracers;
    public final boolean waypoints;
    public final Optional<MinecraftSession.Mode> sessionMode;
    public final Optional<URL> serverUrl;

    public DebugConfiguration(boolean tracers, boolean waypoints, MinecraftSession.Mode sessionMode, URL serverUrl) {
        this.tracers = tracers;
        this.waypoints = waypoints;
        this.sessionMode = Optional.ofNullable(sessionMode);
        this.serverUrl = Optional.ofNullable(serverUrl);
    }

    public static DebugConfiguration load(HomeDirectory home) throws IOException {
        if (!home.debugFile().exists()) {
            return new DebugConfiguration(false, false, null, null);
        }
        Properties properties = new Properties();
        try (InputStream is = new FileInputStream(home.debugFile())) {
            properties.load(is);
        }
        String url = properties.getProperty("server.url", null);
        String mode = properties.getProperty("session.mode", null);
        MinecraftSession.Mode sessionMode = mode == null ? null : MinecraftSession.Mode.valueOf(mode.toUpperCase());
        return new DebugConfiguration(
                Boolean.parseBoolean(properties.getProperty("tracers", "false")),
                Boolean.parseBoolean(properties.getProperty("waypoints", "false")),
                sessionMode,
                url == null ? null : new URL(url)
        );
    }
}
