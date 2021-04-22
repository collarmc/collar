package team.catgirl.collar.client.api.groups;

import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.ApiListener;

/**
 * Listener for the {@link GroupsApi}
 */
public interface GroupsListener extends ApiListener {
    /**
     * Fired when a group is created
     * @param collar client
     * @param groupsApi api
     * @param group created group
     */
    default void onGroupCreated(Collar collar, GroupsApi groupsApi, Group group) {}

    /**
     * Fired when a group is joined by a player
     * @param collar client
     * @param groupsApi api
     * @param group joined
     * @param player joining
     */
    default void onGroupJoined(Collar collar, GroupsApi groupsApi, Group group, Player player) {}

    /**
     * Fired when a group is left by a player
     * @param collar client
     * @param groupsApi api
     * @param group left
     * @param player leaving
     */
    default void onGroupLeft(Collar collar, GroupsApi groupsApi, Group group, Player player) {}

    /**
     * Fired when a group is invited
     * @param collar client
     * @param groupsApi api
     * @param invitation to group
     */
    default void onGroupInvited(Collar collar, GroupsApi groupsApi, GroupInvitation invitation) {}

    /**
     * Fired when a group member has been updated
     * @param collar client
     * @param groupsApi api
     * @param group the player is a member of
     * @param player that was updated
     */
    default void onGroupMemberUpdated(Collar collar, GroupsApi groupsApi, Group group, Player player) {}
}
