package com.collarmc.server.security;

import com.collarmc.protocol.signal.SendPreKeysRequest;
import com.collarmc.protocol.signal.SendPreKeysResponse;
import com.collarmc.security.ClientIdentity;
import com.collarmc.security.ServerIdentity;
import com.collarmc.security.cipher.Cipher;

import java.util.UUID;

public interface ServerIdentityStore {
    /**
     * @return identity of the server
     */
    ServerIdentity getIdentity();

    /**
     * Creates a new identity for a client
     * @param req to create
     */
    void trustIdentity(SendPreKeysRequest req);

    /**
     * Tests if the identity trusted
     * @param identity to test
     * @return trusted or not
     */
    boolean isTrustedIdentity(ClientIdentity identity);

    /**
     * @return creates a new {@link Cipher}
     */
    Cipher createCypher();

    SendPreKeysResponse createSendPreKeysResponse();

    UUID findIdentity(ClientIdentity identity, int deviceId);
}
