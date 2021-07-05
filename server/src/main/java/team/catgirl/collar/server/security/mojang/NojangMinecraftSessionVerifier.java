package team.catgirl.collar.server.security.mojang;

import team.catgirl.collar.protocol.session.StartSessionRequest;

/**
 * Offline {@link MinecraftSessionVerifier}
 */
public class NojangMinecraftSessionVerifier implements MinecraftSessionVerifier {

    public static final String NAME = "nojang";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean verify(StartSessionRequest request) {
        return true;
    }
}
