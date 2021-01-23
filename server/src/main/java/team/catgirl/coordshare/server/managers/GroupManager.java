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
import java.util.logging.Logger;

public final class GroupManager {

    private static final Logger LOGGER = Logger.getLogger(GroupManager.class.getName());

    private final SessionManager sessionManager;
    private final SecureRandom random;

    private final ConcurrentMap<String, Group> groupsById = new ConcurrentHashMap<>();
    private final ArrayListMultimap<UUID, Group> playerToGroups = ArrayListMultimap.create();

    public GroupManager(SessionManager sessionManager) throws NoSuchAlgorithmException {
        this.sessionManager = sessionManager;
        this.random = SecureRandom.getInstance("SHA1PRNG");
    }

    public CreateGroupResponse createGroup(CreateGroupRequest req) {
        Group group = Group.newGroup(uniqueId(), req.me, req.position, req.players);
        synchronized (group.id) {
            playerToGroups.put(req.me.player, group);
            refreshGroupState(group);
            req.players.forEach(player -> {
                Session session = sessionManager.getSession(player);
                if (session != null) {
                    try {
                        sessionManager.send(session, new GroupMembershipRequest(group.id, req.me.player, player, group.members.keySet().asList()).serverMessage());
                    } catch (IOException e) {
                        sessionManager.stopSession(session, "Could not communicate with player " + player, e);
                    }
                }
            });
        }
        return new CreateGroupResponse(group.id);
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
        return new AcceptGroupMembershipResponse(group.id);
    }

    public LeaveGroupResponse leaveGroup(LeaveGroupRequest req) {
        Group group = groupsById.get(req.groupId);
        if (group == null) {
            return new LeaveGroupResponse(null);
        }
        synchronized (group.id) {
            group = group.removeMember(req.me.player);
            playerToGroups.remove(req.me.player, group);
            refreshGroupState(group);
        }
        sendMessageToGroup(req.me.player, group, new UpdatePositionResponse(List.of(group)).serverMessage());
        return new LeaveGroupResponse(group.id);
    }

    public UpdatePositionResponse updatePosition(UpdatePositionRequest req) {
        List<Group> groups = playerToGroups.get(req.me.player);
        if (groups == null) {
            return new UpdatePositionResponse(null);
        }
        List.copyOf(groups).forEach(group -> {
            synchronized (group.id) {
                group = group.updateMemberPosition(req.me.player, req.position);
                refreshGroupState(group);
            }
            sendMessageToGroup(req.me.player, group, new UpdatePositionResponse(groups).serverMessage());
        });
        return new UpdatePositionResponse(playerToGroups.get(req.me.player));
    }

    private void sendMessageToGroup(UUID currentPlayer, Group group, CoordshareServerMessage message) {
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

    private void refreshGroupState(Group group) {
        groupsById.put(group.id, group);
        group.members.keySet().forEach(uuid -> {
            playerToGroups.put(uuid, group);
        });
    }

    private String uniqueId() {
        byte[] values = new byte[1024];
        random.nextLong();
        return BaseEncoding.base64().encode(values);
    }
}
