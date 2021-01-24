package team.catgirl.coordshare.server.managers;

import com.google.common.collect.ImmutableList;
import com.google.common.io.BaseEncoding;
import org.eclipse.jetty.websocket.api.Session;
import team.catgirl.coordshare.messages.ClientMessage.*;
import team.catgirl.coordshare.messages.ServerMessage;
import team.catgirl.coordshare.messages.ServerMessage.*;
import team.catgirl.coordshare.models.Group;
import team.catgirl.coordshare.models.Group.Member;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class GroupManager {

    private static final Logger LOGGER = Logger.getLogger(GroupManager.class.getName());

    private final SessionManager sessionManager;
    private final SecureRandom random;

    private final ConcurrentMap<String, Group> groupsById = new ConcurrentHashMap<>();

    public GroupManager(SessionManager sessionManager) throws NoSuchAlgorithmException {
        this.sessionManager = sessionManager;
        this.random = SecureRandom.getInstance("SHA1PRNG");
    }

    /**
     * Create a new group
     * @param req of the new group request
     * @return response to send to client
     */
    public CreateGroupResponse createGroup(CreateGroupRequest req) {
        Group group = Group.newGroup(uniqueId(), req.me, req.position, req.players);
        synchronized (group.id) {
            refreshGroupState(group);
        }
        return new CreateGroupResponse(group);
    }

    /**
     * Accept a membership request
     * @param req of the new group request
     * @return response to send the client
     */
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

    /**
     * Leave the group
     * @param req to leave the group
     * @return response to client
     */
    public LeaveGroupResponse leaveGroup(LeaveGroupRequest req) {
        Group group = groupsById.get(req.groupId);
        if (group == null) {
            LOGGER.log(Level.INFO, req.me.player + " was not a member of the group " + req.groupId);
            return new LeaveGroupResponse(null);
        }
        synchronized (group.id) {
            group = group.removeMember(req.me.player);
            refreshGroupState(group);
            sendMessageToGroup(req.me.player, group, new UpdatePlayerStateResponse(ImmutableList.of(group)).serverMessage());
        }
        LOGGER.log(Level.INFO, "Group count " + groupsById.size());
        return new LeaveGroupResponse(group.id);
    }

    /**
     * Removes player from all groups
     * @param player to remove
     */
    public void removeUserFromAllGroups(UUID player) {
        List<Group> groups = findGroupsForPlayer(player);
        groups.forEach(group -> {
            synchronized (group.id) {
                group = group.updateMemberState(player, Group.MembershipState.DECLINED);
                refreshGroupState(group);
            }
        });
        LOGGER.log(Level.INFO, "Removed user " + player + " from all groups");
    }

    /**
     * Sends membership requests to the group members
     * @param requester whos sending the request
     * @param group the group to invite to
     * @param members members to send requests to. If null, defaults to the full member list.
     */
    public void sendMembershipRequests(UUID requester, Group group, List<Member> members) {
        synchronized (group.id) {
            Collection<Member> memberList = members == null ? group.members.values() : members;
            memberList.stream().filter(member -> member.membershipState == Group.MembershipState.PENDING).map(member -> member.player).forEach(player -> {
                Session session = sessionManager.getSession(player);
                if (session != null) {
                    try {
                        sessionManager.send(session, new GroupMembershipRequest(group.id, requester, ImmutableList.copyOf(group.members.keySet())).serverMessage());
                    } catch (IOException e) {
                        LOGGER.log(Level.INFO, "Problem sending membership requests", e);
                        sessionManager.stopSession(session, "Could not communicate with player " + player, e);
                    }
                }
            });
        }
    }

    /**
     * Invite user to a group
     * @param groupInviteRequest
     */
    public GroupInviteResponse invite(GroupInviteRequest groupInviteRequest) {
        Group group = groupsById.get(groupInviteRequest.groupId);
        if (group == null) {
            return new GroupInviteResponse(null, null);
        }
        List<Member> newMembers = new ArrayList<>();
        synchronized (group.id) {
            Member requester = group.members.get(groupInviteRequest.me.player);
            if (requester == null) {
                LOGGER.log(Level.INFO, groupInviteRequest.me.player + " is not a member of the group "  + group.id);
                return new GroupInviteResponse(group.id, null);
            }
            if (requester.membershipRole != Group.MembershipRole.OWNER) {
                LOGGER.log(Level.INFO, groupInviteRequest.me.player + " is not OWNER member of the group "  + group.id);
                return new GroupInviteResponse(group.id, null);
            }
            group = group.addMembers(groupInviteRequest.players, Group.MembershipRole.MEMBER, Group.MembershipState.PENDING, (newGroup, members) -> {
                sendMembershipRequests(groupInviteRequest.me.player, newGroup, members);
                newMembers.addAll(members);
            });
            refreshGroupState(group);
        }
        return new GroupInviteResponse(group.id, newMembers.stream().map(member -> member.player).collect(Collectors.toList()));
    }

    /**
     * Update the player state
     * @param req to update player position
     * @return response to send to client
     */
    public UpdatePlayerStateResponse updatePosition(UpdatePlayerStateRequest req) {
        List<Group> groups = findGroupsForPlayer(req.me.player);
        groups.forEach(group -> {
            synchronized (group.id) {
                group = group.updateMemberPosition(req.me.player, req.position);
                refreshGroupState(group);
                sendMessageToGroup(req.me.player, group, new UpdatePlayerStateResponse(groups).serverMessage());
            }
        });
        return new UpdatePlayerStateResponse(findGroupsForPlayer(req.me.player));
    }

    public void updateGroup(String groupId) {
        Group group = groupsById.get(groupId);
        if (group != null) {
            synchronized (group.id) {
                group.members.values().forEach(member -> {
                    List<Group> groupsForPlayer = findGroupsForPlayer(member.player);
                    if (!groupsForPlayer.isEmpty()) {
                        sendMessageToGroup(null, group, new UpdatePlayerStateResponse(groupsForPlayer).serverMessage());
                    }
                });
            }
        }
    }

    private List<Group> findGroupsForPlayer(UUID player) {
        return groupsById.values().stream().filter(group -> group.members.containsKey(player)).collect(Collectors.toList());
    }

    private void sendMessageToGroup(UUID currentPlayer, Group group, ServerMessage message) {
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
                updateGroup(group.id);
            }
        }
    }

    private String uniqueId() {
        byte[] bytes = new byte[64];
        random.nextBytes(bytes);
        return BaseEncoding.base64().encode(bytes);
    }
}
