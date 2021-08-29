package com.collarmc.client.events;

import com.collarmc.client.Collar;
import com.collarmc.client.security.ClientIdentityStore;

/**
 * Fired when the server or client cannot negotiate trust with the client
 * When this occurs, reset the store using {@link ClientIdentityStore#reset()} and reconnect.
 */
public class ClientUntrustedEvent extends AbstractCollarEvent {
    public final ClientIdentityStore identityStore;

    public ClientUntrustedEvent(Collar collar, ClientIdentityStore identityStore) {
        super(collar);
        this.identityStore = identityStore;
    }
}
