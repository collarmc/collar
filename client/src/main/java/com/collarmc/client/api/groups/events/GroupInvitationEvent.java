package com.collarmc.client.api.groups.events;

import com.collarmc.client.Collar;
import com.collarmc.client.api.groups.GroupInvitation;
import com.collarmc.client.events.AbstractCollarEvent;

/**
 * Fired when a group is invited
 */
public final class GroupInvitationEvent extends AbstractCollarEvent {
    public final GroupInvitation invitation;

    public GroupInvitationEvent(Collar collar, GroupInvitation invitation) {
        super(collar);
        this.invitation = invitation;
    }
}
