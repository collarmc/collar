package com.collarmc.server.protocol;

import com.collarmc.protocol.friends.*;
import com.collarmc.server.services.friends.FriendsService;
import com.collarmc.server.services.profiles.ProfileCache;
import org.eclipse.jetty.websocket.api.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.collarmc.api.friends.Friend;
import com.collarmc.api.friends.Status;
import com.collarmc.api.http.RequestContext;
import com.collarmc.api.profiles.PublicProfile;
import com.collarmc.api.session.Player;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.security.ClientIdentity;
import com.collarmc.security.ServerIdentity;
import com.collarmc.server.CollarServer;
import com.collarmc.server.session.SessionManager;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

public class FriendsProtocolHandler extends ProtocolHandler {

    private static final Logger LOGGER = LogManager.getLogger(FriendsProtocolHandler.class.getName());

    private final ServerIdentity serverIdentity;
    private final ProfileCache profiles;
    private final FriendsService friends;
    private final SessionManager sessions;

    public FriendsProtocolHandler(ServerIdentity serverIdentity, ProfileCache profiles, FriendsService friends, SessionManager sessions) {
        this.serverIdentity = serverIdentity;
        this.profiles = profiles;
        this.friends = friends;
        this.sessions = sessions;
    }

    @Override
    public boolean handleRequest(CollarServer collar, ProtocolRequest req, BiConsumer<ClientIdentity, ProtocolResponse> sender) {
        if (req instanceof AddFriendRequest) {
            AddFriendRequest request = (AddFriendRequest) req;
            findFriendProfileId(request.profile, request.player).ifPresentOrElse(friendProfileId -> {
                Friend friend = friends.createFriend(RequestContext.from(req.identity), new FriendsService.CreateFriendRequest(request.identity.profile, friendProfileId)).friend;
                sender.accept(req.identity, new AddFriendResponse(serverIdentity, friend));
            }, () -> {
                LOGGER.error("Could not add friend with profileId " + request.profile  + " or playerId " + request.player);
            });
            return true;
        } else if (req instanceof RemoveFriendRequest) {
            RemoveFriendRequest request = (RemoveFriendRequest) req;
            findFriendProfileId(request.profile, request.player).ifPresentOrElse(friendProfileId -> {
                UUID deletedFriend = friends.deleteFriend(RequestContext.from(req.identity), new FriendsService.DeleteFriendRequest(req.identity.profile, friendProfileId)).friend;
                sender.accept(req.identity, new RemoveFriendResponse(serverIdentity, deletedFriend));
            }, () -> {
                LOGGER.error("Could not add friend with profileId " + request.profile + " or playerId " + request.player);
            });
            return true;
        } else if (req instanceof GetFriendListRequest) {
            List<Friend> friends = this.friends.getFriends(RequestContext.from(req.identity), new FriendsService.GetFriendsRequest(req.identity.profile, null)).friends;
            sender.accept(req.identity, new GetFriendListResponse(serverIdentity, friends));
            return true;
        }
        return false;
    }

    private Optional<UUID> findFriendProfileId(UUID profile, UUID player) {
        Optional<UUID> profileId;
        if (profile != null) {
            profileId = Optional.of(profile);
        } else if (player != null) {
            profileId = sessions.getSessionStateByPlayer(player).map(sessionState -> sessionState.identity.profile);
        } else {
            profileId = Optional.empty();
        }
        return profileId;
    }

    @Override
    public void onSessionStopping(ClientIdentity identity, Player player, BiConsumer<Session, ProtocolResponse> sender) {
        // Broadcast to the players friends that the player is now offline
        friends.getFriends(RequestContext.from(identity), new FriendsService.GetFriendsRequest(null, identity.profile)).friends.forEach(friend -> {
            sessions.getSessionStateByOwner(friend.owner).ifPresentOrElse(sessionState -> {
                PublicProfile profile = profiles.getById(identity.profile).orElseThrow(() -> new IllegalStateException("could not find profile " + identity.profile)).toPublic();
                Friend offline = new Friend(sessionState.identity.profile, profile, Status.OFFLINE, Set.of());
                LOGGER.info("Notifying " + sessionState.identity + " that player " + identity + " is OFFLINE");
                sender.accept(sessionState.session, new FriendChangedResponse(serverIdentity, offline));
            }, () -> {
                LOGGER.info("No friends to notify");
            });
        });
    }
}
