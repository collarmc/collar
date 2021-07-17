package com.collarmc.server.security.mojang;

import com.collarmc.http.HttpClient;
import com.collarmc.protocol.session.StartSessionRequest;
import com.collarmc.security.mojang.Mojang;

/**
 * Verifies identifies against Mojang auth servers
 */
public class MojangMinecraftSessionVerifier implements MinecraftSessionVerifier {

    private static final String NAME = "mojang";

    private final HttpClient http;

    public MojangMinecraftSessionVerifier(HttpClient http) {
        this.http = http;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean verify(StartSessionRequest request) {
        return new Mojang(http).hasJoined(request.session, request.serverId);
    }
}
