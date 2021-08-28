package com.collarmc.server.protocol;

import com.collarmc.api.friends.Friend;
import com.collarmc.api.friends.Status;
import com.collarmc.api.http.RequestContext;
import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.profiles.PublicProfile;
import com.collarmc.api.session.Player;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.protocol.friends.*;
import com.collarmc.server.CollarServer;
import com.collarmc.server.Services;
import com.collarmc.server.services.friends.FriendsService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

public class FriendsProtocolHandler extends ProtocolHandler {

    private static final Logger LOGGER = LogManager.getLogger(FriendsProtocolHandler.class.getName());

    public FriendsProtocolHandler(Services services) {
        super(services);
    }

    @Override
    public boolean handleRequest(CollarServer collar, ClientIdentity identity, ProtocolRequest req, BiConsumer<ClientIdentity, ProtocolResponse> sender) {
        RequestContext caller = RequestContext.from(identity);
        if (req instanceof AddFriendRequest) {
            AddFriendRequest request = (AddFriendRequest) req;
            findFriendProfileId(request.profile, request.player).ifPresentOrElse(friendProfileId -> {
                Friend friend = services.friends.createFriend(caller, new FriendsService.CreateFriendRequest(identity.id(), friendProfileId)).friend;
                sender.accept(identity, new AddFriendResponse(friend));
            }, () -> {
                LOGGER.error("Could not add friend with profileId " + request.profile  + " or playerId " + request.player);
            });
            return true;
        } else if (req instanceof RemoveFriendRequest) {
            RemoveFriendRequest request = (RemoveFriendRequest) req;
            findFriendProfileId(request.profile, request.player).ifPresentOrElse(friendProfileId -> {
                UUID deletedFriend = services.friends.deleteFriend(caller, new FriendsService.DeleteFriendRequest(identity.id(), friendProfileId)).friend;
                sender.accept(identity, new RemoveFriendResponse(deletedFriend));
            }, () -> {
                LOGGER.error("Could not add friend with profileId " + request.profile + " or playerId " + request.player);
            });
            return true;
        } else if (req instanceof GetFriendListRequest) {
            List<Friend> friends = services.friends.getFriends(caller, new FriendsService.GetFriendsRequest(identity.id(), null)).friends;
            sender.accept(identity, new GetFriendListResponse(friends));
            return true;
        }
        return false;
    }

    private Optional<UUID> findFriendProfileId(UUID profile, UUID player) {
        Optional<UUID> profileId;
        if (profile != null) {
            profileId = Optional.of(profile);
        } else if (player != null) {
            profileId = services.sessions.getSessionStateByPlayer(player).map(sessionState -> sessionState.identity.id());
        } else {
            profileId = Optional.empty();
        }
        return profileId;
    }

    @Override
    public void onSessionStopping(ClientIdentity identity, Player player, BiConsumer<Session, ProtocolResponse> sender) {
        // Broadcast to the players friends that the player is now offline
        services.friends.getFriends(RequestContext.from(identity), new FriendsService.GetFriendsRequest(null, identity.id())).friends.forEach(friend -> {
            services.sessions.getSessionStateByOwner(friend.owner).ifPresentOrElse(sessionState -> {
                PublicProfile profile = services.profileCache.getById(identity.id()).orElseThrow(() -> new IllegalStateException("could not find profile " + identity.id())).toPublic();
                Friend offline = new Friend(sessionState.identity.id(), profile, Status.OFFLINE, Set.of());
                LOGGER.info("Notifying " + sessionState.identity + " that player " + identity + " is OFFLINE");
                sender.accept(sessionState.session, new FriendChangedResponse(offline));
            }, () -> {
                LOGGER.info("No friends to notify");
            });
        });
    }
}
