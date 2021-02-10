package team.catgirl.collar.server.protocol;

import team.catgirl.collar.api.friends.Friend;
import team.catgirl.collar.api.friends.Status;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.friends.*;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.server.CollarServer;
import team.catgirl.collar.server.services.friends.FriendsService;
import team.catgirl.collar.server.services.friends.FriendsService.CreateFriendRequest;
import team.catgirl.collar.server.services.friends.FriendsService.DeleteFriendRequest;
import team.catgirl.collar.server.session.SessionManager;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FriendsProtocolHandler extends ProtocolHandler {

    private static final Logger LOGGER = Logger.getLogger(FriendsProtocolHandler.class.getName());

    private final ServerIdentity serverIdentity;
    private final FriendsService friends;
    private final SessionManager sessions;

    public FriendsProtocolHandler(ServerIdentity serverIdentity, FriendsService friends, SessionManager sessions) {
        this.serverIdentity = serverIdentity;
        this.friends = friends;
        this.sessions = sessions;
    }

    @Override
    public boolean handleRequest(CollarServer collar, ProtocolRequest req, Consumer<ProtocolResponse> sender) {
        if (req instanceof AddFriendRequest) {
            AddFriendRequest request = (AddFriendRequest) req;
            UUID profileId;
            if (request.profile != null) {
                profileId = request.profile;
            } else if (request.player != null) {
                profileId = sessions.getSessionStateByPlayer(request.player).map(sessionState -> sessionState.identity.owner).orElse(null);
            }  else {
                profileId = null;
            }
            if (profileId == null) {
                LOGGER.log(Level.SEVERE, "Could not add friend for " + req.identity);
                return true;
            }
            Friend friend = friends.createFriend(new CreateFriendRequest(request.identity.owner, profileId)).friend;
            sender.accept(new AddFriendResponse(serverIdentity, friend));
            return true;
        } else if (req instanceof RemoveFriendRequest) {
            RemoveFriendRequest request = (RemoveFriendRequest) req;
            UUID profileId;
            if (request.profile != null) {
                profileId = request.profile;
            } else if (request.player != null) {
                profileId = sessions.getSessionStateByPlayer(request.player).map(sessionState -> sessionState.identity.owner).orElse(null);
            }  else {
                profileId = null;
            }
            if (profileId == null) {
                LOGGER.log(Level.SEVERE, "Could not add friend for " + req.identity);
                return true;
            }
            UUID deletedFriend = friends.deleteFriend(new DeleteFriendRequest(request.identity.owner, profileId)).friend;
            sender.accept(new RemoveFriendResponse(serverIdentity, deletedFriend));
            return true;
        } else if (req instanceof GetFriendListRequest) {
            List<Friend> friends = this.friends.getFriends(new FriendsService.GetFriendsRequest(req.identity.owner, null)).friends;
            sender.accept(new GetFriendListResponse(serverIdentity, friends));
            return true;
        }
        return false;
    }

    @Override
    public void onSessionStopped(ClientIdentity identity, MinecraftPlayer player, Consumer<ProtocolResponse> sender) {
        // Broadcast to friends that you are offline
        friends.getFriends(new FriendsService.GetFriendsRequest(null, identity.owner)).friends.forEach(friend -> {
            friend.playerIds.forEach(uuid -> sender.accept(new FriendChangedResponse(serverIdentity, new Friend(identity.owner, Status.OFFLINE, List.of()))));
        });
    }
}
