package com.collarmc.security.messages;

import com.collarmc.api.identity.Identity;

public interface IdentityStore<T extends Identity> {
    /**
     * @return identity of this store
     */
    T identity();

    /**
     * @return creates a new {@link Cipher}
     */
    Cipher<T> cipher();
}
