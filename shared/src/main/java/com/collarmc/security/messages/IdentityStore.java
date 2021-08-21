package com.collarmc.security.messages;

import com.collarmc.api.identity.Identity;

public interface IdentityStore<T extends Identity> {
    /**
     * @return identity of this store
     */
    T identity();

    /**
     * Creates a new identity for a client
     * @param identity to create
     */
    void trustIdentity(Identity identity);

    /**
     * Tests if the identity trusted
     * @param identity to test
     * @return trusted or not
     */
    boolean isTrustedIdentity(Identity identity);

    /**
     * @return creates a new {@link Cipher}
     */
    Cipher<T> cipher();
}
