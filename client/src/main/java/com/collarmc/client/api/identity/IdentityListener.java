package com.collarmc.client.api.identity;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.client.Collar;
import com.collarmc.client.api.ApiListener;
import com.collarmc.client.security.ClientIdentityStore;

public interface IdentityListener extends ApiListener {
    /**
     * Fired when an identity is trusted by the client
     * @param collar client
     * @param api identity
     * @param identityStore identity store for the client
     * @param identity trusted
     */
    default void onIdentityTrusted(Collar collar, IdentityApi api, ClientIdentityStore identityStore, ClientIdentity identity) {}
}
