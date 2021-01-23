package team.catgirl.coordshare.server.managers;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.io.BaseEncoding;
import org.eclipse.jetty.websocket.api.Session;
import team.catgirl.coordshare.models.CoordshareServerMessage;
import team.catgirl.coordshare.models.CoordshareServerMessage.*;
import team.catgirl.coordshare.models.Group;
import team.catgirl.coordshare.models.CoordshareClientMessage.AcceptGroupMembershipRequest;
import team.catgirl.coordshare.models.CoordshareClientMessage.CreateGroupRequest;
import team.catgirl.coordshare.models.CoordshareClientMessage.LeaveGroupRequest;
import team.catgirl.coordshare.models.CoordshareClientMessage.UpdatePositionRequest;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class GroupManager {

    private static final Logger LOGGER = Logger.getLogger(GroupManager.class.getName());

    private final SessionManager sessionManager;
    private final SecureRandom random;

    private final ConcurrentMap<String, Group> groupsById = new ConcurrentHashMap<>();
//    private final ArrayListMultimap<UUID, Group> playerToGroups = ArrayListMultimap.create();

    public GroupManager(SessionManager sessionManager) throws NoSuchAlgorithmException {
        this.sessionManager = sessionManager;
        this.random = SecureRandom.getInstance("SHA1PRNG");
    }

    public CreateGroupResponse createGroup(CreateGroupRequest req) {
        Group group = Group.newGroup(uniqueId(), req.me, req.position, req.players);
        synchronized (group.id) {
            refreshGroupState(group);
        }
        return new CreateGroupResponse(group);
    }

    public AcceptGroupMembershipResponse acceptMembership(AcceptGroupMembershipRequest req) {
        Group group = groupsById.get(req.groupId);
        if (group == null) {
            return new AcceptGroupMembershipResponse(null);
        }
        synchronized (group.id) {
            Group.MembershipState state = req.state;
            group = group.updateMemberState(req.me.player, state);
            refreshGroupState(group);
        }
        return new AcceptGroupMembershipResponse(group);
    }

    public LeaveGroupResponse leaveGroup(LeaveGroupRequest req) {
        Group group = groupsById.get(req.groupId);
        if (group == null) {
            return new LeaveGroupResponse(null);
        }
        synchronized (group.id) {
            group = group.removeMember(req.me.player);
            refreshGroupState(group);
            sendMessageToGroup(req.me.player, group, new UpdateGroupStateResponse(List.of(group)).serverMessage());
        }
        LOGGER.log(Level.INFO, "Group count " + groupsById.size());
        return new LeaveGroupResponse(group.id);
    }

    public void removeUser(UUID player) {
        List<Group> groups = findGroupsForPlayer(player);
        groups.forEach(group -> {
            synchronized (group.id) {
                group = group.updateMemberState(player, null);
                refreshGroupState(group);
            }
        });
        LOGGER.log(Level.INFO, "Removed user " + player + " from all groups");
    }

    public void sendMembershipRequests(UUID requester, Group group) {
        synchronized (group.id) {
            group.members.values().stream().filter(member -> member.membershipState == Group.MembershipState.PENDING).map(member -> member.player).forEach(player -> {
                Session session = sessionManager.getSession(player);
                if (session != null) {
                    try {
                        sessionManager.send(session, new GroupMembershipRequest(group.id, requester, group.members.keySet().asList()).serverMessage());
                    } catch (IOException e) {
                        sessionManager.stopSession(session, "Could not communicate with player " + player, e);
                    }
                }
            });
        }
    }

    public UpdateGroupStateResponse updatePosition(UpdatePositionRequest req) {
        List<Group> groups = findGroupsForPlayer(req.me.player);
        groups.forEach(group -> {
            synchronized (group.id) {
                group = group.updateMemberPosition(req.me.player, req.position);
                refreshGroupState(group);
                sendMessageToGroup(req.me.player, group, new UpdateGroupStateResponse(groups).serverMessage());
            }
        });
        return new UpdateGroupStateResponse(findGroupsForPlayer(req.me.player));
    }

    public void updateGroup(String groupId) {
        Group group = groupsById.get(groupId);
        if (group != null) {
            synchronized (group.id) {
                group.members.values().forEach(member -> {
                    List<Group> groupsForPlayer = findGroupsForPlayer(member.player);
                    if (!groupsForPlayer.isEmpty()) {
                        sendMessageToGroup(null, group, new UpdateGroupStateResponse(groupsForPlayer).serverMessage());
                    }
                });
            }
        }
    }

    private List<Group> findGroupsForPlayer(UUID player) {
        return groupsById.values().stream().filter(group -> group.members.containsKey(player)).collect(Collectors.toList());
    }

    private void sendMessageToGroup(UUID currentPlayer, Group group, CoordshareServerMessage message) {
        synchronized (group.id) {
            group.members.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(currentPlayer))
                    .filter(entry -> entry.getValue().membershipState == Group.MembershipState.ACCEPTED)
                    .forEach(entry -> {
                        Session session = sessionManager.getSession(entry.getKey());
                        if (session != null) {
                            try {
                                sessionManager.send(session, message);
                            } catch (IOException e) {
                                sessionManager.stopSession(session, "Could not communicate with player " + entry.getKey(), e);
                            }
                        }
                    });
        }
    }

    private void refreshGroupState(Group group) {
        synchronized (group.id) {
            if (group.members.isEmpty()) {
                LOGGER.log(Level.INFO, "Removed group " + group.id + " as it has no members.");
                groupsById.remove(group.id);
            } else {
                groupsById.put(group.id, group);
            }
        }
    }

    private String uniqueId() {
        byte[] bytes = new byte[64];
        random.nextBytes(bytes);
        return BaseEncoding.base64().encode(bytes);
    }
}
