package com.collarmc.client.api.groups;

import com.collarmc.api.friends.Status;
import com.collarmc.api.groups.*;
import com.collarmc.api.session.Player;
import com.collarmc.client.Collar;
import com.collarmc.client.api.AbstractApi;
import com.collarmc.client.api.groups.events.*;
import com.collarmc.client.sdht.SDHTApi;
import com.collarmc.client.security.ClientIdentityStore;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.protocol.groups.*;
import com.collarmc.security.messages.GroupSession;
import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class GroupsApi extends AbstractApi {
    private final ConcurrentMap<UUID, Group> groups = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, GroupSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, GroupInvitation> invitations = new ConcurrentHashMap<>();

    private static final Logger LOGGER = LogManager.getLogger(GroupsApi.class.getName());
    private final SDHTApi sdhtApi;

    public GroupsApi(Collar collar,
                     Supplier<ClientIdentityStore> identityStoreSupplier,
                     Consumer<ProtocolRequest> sender,
                     SDHTApi sdhtApi) {
        super(collar, identityStoreSupplier, sender);
        this.sdhtApi = sdhtApi;
    }

    /**
     * @return groups the client is a member of
     */
    public List<Group> all() {
        synchronized (this) {
            return ImmutableList.copyOf(groups.values());
        }
    }

    /**
     * @return all {@link GroupType#GROUP}
     */
    public List<Group> groups() {
        synchronized (this) {
            return filter(GroupType.GROUP);
        }
    }

    /**
     * @return all {@link GroupType#PARTY}
     */
    public List<Group> parties() {
        synchronized (this) {
            return filter(GroupType.PARTY);
        }
    }

    /**
     * @return all {@link GroupType#NEARBY}
     */
    public List<Group> nearby() {
        synchronized (this) {
            return filter(GroupType.NEARBY);
        }
    }

    /**
     * @param types to filter by
     * @return groups matching types
     */
    public List<Group> matching(GroupType... types) {
        List<GroupType> filterTypes = Arrays.asList(types);
        synchronized (this) {
            return all().stream().filter(group -> filterTypes.contains(group.type)).collect(Collectors.toList());
        }
    }

    /**
     * Find a group by its ID
     * @param groupId of the group to find
     * @return group
     */
    public Optional<Group> findGroupById(UUID groupId) {
        return groups.values().stream().filter(group -> group.id.equals(groupId)).findFirst();
    }

    /**
     * @return pending invitations
     */
    public List<GroupInvitation> invitations() {
        synchronized (this) {
            return new ArrayList<>(invitations.values());
        }
    }

    /**
     * Create a group with other players
     * @param players players
     */
    public void create(String name, GroupType type, List<UUID> players) {
        sender.accept(new CreateGroupRequest(UUID.randomUUID(), name, type, players));
    }

    /**
     * Leave the group
     * @param group the group to leave
     */
    public void leave(Group group) {
        sender.accept(new LeaveGroupRequest(group.id));
    }

    /**
     * Invite players to a group
     * @param group to invite players to
     * @param players to invite
     */
    public void invite(Group group, List<UUID> players) {
        sender.accept(new GroupInviteRequest(group.id, players));
    }

    /**
     * Invite players to a group
     * @param group to invite players to
     * @param players to invite
     */
    public void invite(Group group, UUID... players) {
        invite(group, Arrays.asList(players));
    }

    /**
     * Accept an invitation
     * @param invitation to accept
     */
    public void accept(GroupInvitation invitation) {
        sender.accept(identityStore().createJoinGroupRequest(invitation.group));
        invitations.remove(invitation.group);
    }

    /**
     * Remove the member from the group.
     * Only {@link MembershipRole#OWNER} can perform this action.
     * @param group to remove player from
     * @param member the member to remove
     */
    public void removeMember(Group group, Member member) {
        sender.accept(new EjectGroupMemberRequest(group.id, member.player.minecraftPlayer.id));
    }

    /**
     * Delete a group
     * @param group to delete
     */
    public void delete(Group group) {
        sender.accept(new DeleteGroupRequest(group.id));
    }

    /**
     * Transfer ownership of the group to another player
     * @param group to transfer
     * @param player to transfer to
     */
    public void transferOwnership(Group group, Player player) {
        sender.accept(new TransferGroupOwnershipRequest(group.id, player.identity.id()));
    }

    private List<Group> filter(GroupType party) {
        return groups.values().stream().filter(group -> group.type == party).collect(Collectors.toList());
    }

    @Override
    public void onStateChanged(Collar.State state) {
        if (state == Collar.State.DISCONNECTED) {
            synchronized (this) {
                groups.clear();
                invitations.clear();
            }
        }
    }

    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        if (resp instanceof CreateGroupResponse) {
            synchronized (this) {
                CreateGroupResponse response = (CreateGroupResponse)resp;
                Group group = response.group;
                groups.put(response.group.id, group);
                sessions.compute(response.group.id, (uuid, groupSession) -> identityStore().createSession(response.group));
                collar.configuration.eventBus.dispatch(new GroupCreatedEvent(collar, group));
                collar.configuration.eventBus.dispatch(new GroupJoinedEvent(collar, group, collar.player()));
            }
            return true;
        } else if (resp instanceof JoinGroupResponse) {
            synchronized (this) {
                JoinGroupResponse response = (JoinGroupResponse) resp;
                AcknowledgedGroupJoinedRequest request = identityStore().processJoinGroupResponse(response);
                sender.accept(request);
            }
            return true;
        } else if (resp instanceof AcknowledgedGroupJoinedResponse) {
            AcknowledgedGroupJoinedResponse response = (AcknowledgedGroupJoinedResponse) resp;
            groups.put(response.group.id, response.group);
            invitations.remove(response.group.id);
            if (groups.containsKey(response.group.id)) {
                identityStore().processAcknowledgedGroupJoinedResponse(response);
            }
            if (response.player.equals(collar.player())) {
                sdhtApi.table.sync(response.group.id);
            }
            sessions.compute(response.group.id, (uuid, groupSession) -> identityStore().createSession(response.group));
            collar.configuration.eventBus.dispatch(new GroupJoinedEvent(collar, response.group, response.player));
            LOGGER.info("AcknowledgedGroupJoinedResponse triggered by group:" + response.group.id);
        } else if (resp instanceof LeaveGroupResponse) {
            synchronized (this) {
                LeaveGroupResponse response = (LeaveGroupResponse)resp;
                if (response.sender == null || response.sender.equals(collar.identity())) {
                    // Remove myself from the group
                    Group removed = groups.remove(response.groupId);
                    if (removed != null) {
                        sdhtApi.table.remove(removed.id);
                        sessions.remove(removed.id);
                        collar.configuration.eventBus.dispatch(new GroupLeftEvent(collar, removed, response.player));
                    }
                    invitations.remove(response.groupId);
                } else {
                    // Remove another player from the group
                    Group group = groups.get(response.groupId);
                    if (group != null) {
                        Group updatedGroup = group.removeMember(response.player);
                        groups.put(updatedGroup.id, updatedGroup);
                        sessions.compute(updatedGroup.id, (uuid, groupSession) -> {
                            if (groupSession != null) {
                                return groupSession.remove(response.player.identity);
                            }
                            return null;
                        });
                        collar.configuration.eventBus.dispatch(new GroupLeftEvent(collar, updatedGroup, response.player));
                    }
                }
                // Remove the group sessions of the player who left the group so that any messages they send
                // will no longer be decrypted by the client.
                // When this method is called after the client itself has left, it removes the session for the group
                // until they are invited to join the same group again
                identityStore().processLeaveGroupResponse(response);
            }
            return true;
        } else if (resp instanceof GroupInviteResponse) {
            synchronized (this) {
                GroupInviteResponse response = (GroupInviteResponse) resp;
                GroupInvitation invitation = GroupInvitation.from(response);
                switch (response.type) {
                    case GROUP:
                    case PARTY:
                        invitations.put(invitation.group, invitation);
                        collar.configuration.eventBus.dispatch(new GroupInvitationEvent(collar, invitation));
                        break;
                    case NEARBY:
                        // Auto-accept invitations from location typed groups
                        accept(invitation);
                        break;
                    default:
                        throw new IllegalStateException("unknown group type" + response.type);
                }
            }
            return true;
        } else if (resp instanceof RejoinGroupResponse) {
            // Rejoin the group
            sender.accept(identityStore().createJoinGroupRequest(((RejoinGroupResponse) resp).group));
            LOGGER.info("Received RejoinGroupResponse for group: " + ((RejoinGroupResponse) resp).group);
        } else if (resp instanceof UpdateGroupMemberResponse) {
            synchronized (this) {
                UpdateGroupMemberResponse response = (UpdateGroupMemberResponse) resp;
                Group group = groups.get(response.groupId);
                if (group != null) {
                    Group updatedGroup;
                    if (response.status == Status.OFFLINE) {
                        updatedGroup = group.updatePlayer(new MemberSource(response.player, response.profile));
                    } else if (response.role != null) {
                        updatedGroup = group.updateMembershipRole(response.player, response.role);
                    } else {
                        updatedGroup = null;
                    }
                    if (updatedGroup != null) {
                        groups.put(updatedGroup.id, updatedGroup);
                        sessions.compute(updatedGroup.id, (uuid, groupSession) -> {
                            if (groupSession == null) {
                                return null;
                            }
                            switch (response.status) {
                                case ONLINE:
                                    return groupSession.add(response.player.identity);
                                case OFFLINE:
                                default:
                                    return groupSession.remove(response.player.identity);
                            }
                        });
                        collar.configuration.eventBus.dispatch(new GroupMemberUpdatedEvent(collar, updatedGroup, response.player));
                    }
                }
            }
            return true;
        }
        return false;
    }
}
