package com.collarmc.client.events;

import com.collarmc.client.Collar;

/**
 * Fired when the server detects a mismatch between the stored private identity and the one provided by the session
 * User will have to visit a page on the collar web app to confirm that they want to delete encrypted blobs stored
 * against their user or provide the identity file and start again
 */
public final class PrivateIdentityMismatchEvent extends AbstractCollarEvent {
    public final String url;

    public PrivateIdentityMismatchEvent(Collar collar, String url) {
        super(collar);
        this.url = url;
    }
}
