package team.catgirl.coordshare.client;

import team.catgirl.coordshare.messages.ServerMessage;
import team.catgirl.coordshare.messages.ServerMessage.*;

public interface CoordshareListener {
    default void onSessionCreated(CoordshareClient client) {}
    default void onDisconnect(CoordshareClient client) {}
    default void onGroupCreated(CoordshareClient client, CreateGroupResponse resp) {};
    default void onGroupMembershipRequested(CoordshareClient client, GroupMembershipRequest resp) {};
    default void onGroupJoined(CoordshareClient client, AcceptGroupMembershipResponse resp) {};
    default void onGroupLeft(CoordshareClient client, LeaveGroupResponse resp) {};
    default void onGroupUpdated(CoordshareClient client, UpdatePlayerStateResponse resp) {};
    default void onGroupInvitesSent(CoordshareClient client, GroupInviteResponse resp) {};
    default void onPongReceived(ServerMessage.Pong pong) {};
}
