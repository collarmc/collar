package com.collarmc.client.events;

import com.collarmc.client.Collar;

/**
 * Fired on new installation, to confirm that the device is trusted with an authorized collar user
 */
public final class ConfirmClientRegistrationEvent extends AbstractCollarEvent {
    public final String token;
    public final String approvalUrl;

    public ConfirmClientRegistrationEvent(Collar collar, String token, String approvalUrl) {
        super(collar);
        this.token = token;
        this.approvalUrl = approvalUrl;
    }
}
