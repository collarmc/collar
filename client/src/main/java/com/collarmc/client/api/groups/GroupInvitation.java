package com.collarmc.client.api.groups;

import com.collarmc.api.groups.GroupType;
import com.collarmc.api.session.Player;
import com.collarmc.protocol.groups.GroupInviteResponse;

import java.util.UUID;

/**
 * Invitation for a Player to join a Group
 */
public final class GroupInvitation {
    /**
     * The ID of the group
     */
    public final UUID group;
    /**
     * Name of the group
     */
    public final String name;
    /**
     * Type of group
     */
    public final GroupType type;
    /**
     * The player sending the invitation
     * If null, the server has resent a pending invitation
     */
    public final Player sender;

    /**
     * @param group id to join
     * @param name of the group
     * @param type of the group
     * @param sender who sent the invitation (or null if server sent it)
     */
    public GroupInvitation(UUID group,
                           String name,
                           GroupType type,
                           Player sender) {
        this.group = group;
        this.name = name;
        this.type = type;
        this.sender = sender;
    }

    /**
     * Convert the response to an invitation object
     * @param req response
     * @return invitation
     */
    public static GroupInvitation from(GroupInviteResponse req) {
        return new GroupInvitation(req.group, req.name, req.type, req.sender);
    }
}
