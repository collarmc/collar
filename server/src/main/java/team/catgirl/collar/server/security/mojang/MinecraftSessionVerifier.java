package team.catgirl.collar.server.security.mojang;

import team.catgirl.collar.protocol.session.StartSessionRequest;

/**
 * Verification of reported Minecraft session identity
 */
public interface MinecraftSessionVerifier {

    /**
     * The name of the verification scheme
     * @return name
     */
    String getName();

    /**
     * Tests that the provided session is valid
     * @param request to test
     * @return valid
     */
    boolean verify(StartSessionRequest request);
}
