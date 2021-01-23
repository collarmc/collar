package team.catgirl.coordshare.client;

import team.catgirl.coordshare.models.CoordshareServerMessage.CreateGroupResponse;
import team.catgirl.coordshare.models.CoordshareServerMessage.GroupMembershipRequest;
import team.catgirl.coordshare.models.CoordshareServerMessage.LeaveGroupResponse;
import team.catgirl.coordshare.models.CoordshareServerMessage.UpdatePositionResponse;

public interface CoordshareListener {
    default void onConnected(CoordshareClient client) {}
    default void onSessionCreated(CoordshareClient client) {}
    default void onDisconnect(CoordshareClient client) {}
    default void onGroupCreated(CoordshareClient client, CreateGroupResponse resp) {};
    default void onGroupMembershipRequested(CoordshareClient client, GroupMembershipRequest resp) {};
    default void onGroupLeft(CoordshareClient client, LeaveGroupResponse resp) {};
    default void onPositionsUpdated(CoordshareClient client, UpdatePositionResponse updatePositionResponse) {};
}
