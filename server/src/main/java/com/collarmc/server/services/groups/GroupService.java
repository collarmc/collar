package com.collarmc.server.services.groups;

import com.collarmc.api.friends.Status;
import com.collarmc.api.groups.*;
import com.collarmc.api.groups.http.*;
import com.collarmc.api.http.HttpException.BadRequestException;
import com.collarmc.api.http.HttpException.ForbiddenException;
import com.collarmc.api.http.HttpException.NotFoundException;
import com.collarmc.api.http.HttpException.ServerErrorException;
import com.collarmc.api.http.RequestContext;
import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.profiles.Profile;
import com.collarmc.api.profiles.ProfileService;
import com.collarmc.api.profiles.ProfileService.GetProfileRequest;
import com.collarmc.api.profiles.ProfileService.UpdateProfileRequest;
import com.collarmc.api.profiles.PublicProfile;
import com.collarmc.api.profiles.Role;
import com.collarmc.api.session.Player;
import com.collarmc.molib.Mojang;
import com.collarmc.molib.profile.PlayerProfileInfo;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.protocol.groups.*;
import com.collarmc.protocol.messaging.SendMessageRequest;
import com.collarmc.protocol.messaging.SendMessageResponse;
import com.collarmc.security.messages.Cipher;
import com.collarmc.security.messages.CipherException;
import com.collarmc.security.messages.GroupMessage;
import com.collarmc.security.messages.GroupMessageEnvelope;
import com.collarmc.server.protocol.BatchProtocolResponse;
import com.collarmc.server.services.location.NearbyGroups;
import com.collarmc.server.services.profiles.ProfileCache;
import com.collarmc.server.session.SessionManager;
import com.google.common.collect.ImmutableList;
import com.google.common.io.BaseEncoding;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class GroupService {

    private static final Logger LOGGER = LogManager.getLogger(GroupService.class.getName());

    private final GroupStore store;
    private final ProfileService profileService;
    private final ProfileCache profiles;
    private final SessionManager sessions;
    private final Mojang mojang;
    private final Cipher cipher;

    public GroupService(GroupStore store,
                        ProfileService profileService,
                        ProfileCache profiles,
                        SessionManager sessions,
                        Mojang mojang, Cipher cipher) {
        this.store = store;
        this.profileService = profileService;
        this.profiles = profiles;
        this.sessions = sessions;
        this.mojang = mojang;
        this.cipher = cipher;
    }

    /**
     * @param groupIds to find
     * @return the list of matching groups
     */
    public Set<Group> findGroups(Set<UUID> groupIds) {
        return store.findGroups(groupIds).collect(Collectors.toSet());
    }

    /**
     * Create a new group
     * @param req of the new group request
     * @return response to send to client
     */
    public Optional<BatchProtocolResponse> createGroup(ClientIdentity identity, CreateGroupRequest req) {
        if (req.type == GroupType.NEARBY) {
            throw new IllegalStateException("clients cannot create nearby groups");
        }
        if (store.findGroup(req.groupId).isPresent()) {
            throw new IllegalStateException("Group " + req.groupId + " already exists");
        }

        List<MemberSource> players = sessions.findPlayers(identity, req.players).stream().map(player -> {
            PublicProfile profile = profiles.getById(player.identity.id()).orElseThrow(() -> new IllegalStateException("cannot find profile " + player.identity.id())).toPublic();
            return new MemberSource(player, profile);
        }).collect(Collectors.toList());

        return sessions.findPlayer(identity)
                .map(player -> {
                    Profile profile = profiles.getById(player.identity.id()).orElseThrow(() -> new IllegalStateException("cant find player with profile " + player.identity.id()));
                    Group group = Group.newGroup(req.groupId, req.name, req.type, new MemberSource(player, profile.toPublic()), players);
                    List<Member> members = group.members.stream()
                            .filter(member -> member.membershipRole.equals(MembershipRole.MEMBER))
                            .collect(Collectors.toList());
                    store.upsert(group);
                    BatchProtocolResponse response = new BatchProtocolResponse();
                    createGroupMembershipRequests(identity, group, members).ifPresent(response::concat);
                    response.add(identity, new CreateGroupResponse(group));
                    return response;
                });
    }

    /**
     * Delete a group
     * @param req of the delete group request
     * @return response to send to client
     */
    public Optional<ProtocolResponse> delete(ClientIdentity identity, DeleteGroupRequest req) {
        Optional<Group> group = store.findGroup(req.group);
        if (group.isPresent()) {
            Group theGroup = group.get();
            return this.sessions.findPlayer(identity).map(player -> {
                if (theGroup.getRole(player) != MembershipRole.OWNER) {
                    throw new IllegalStateException(identity + " is not owner of group " + theGroup.id);
                }
                store.delete(theGroup.id);
                return createMemberMessages(theGroup, member -> true, (thePlayer, updatedMember) -> new LeaveGroupResponse(theGroup.id, null, null));
            });
        }
        return Optional.empty();
    }

    /**
     * Sends responses to rejoin any groups and re-issue invitations
     * @param identity of the joining player
     * @param player the joining player
     * @return responses to send
     */
    public Optional<BatchProtocolResponse> playerIsOnline(ClientIdentity identity, Player player) {
        BatchProtocolResponse response = new BatchProtocolResponse();
        store.findGroupsContaining(player).forEach(group -> {
            group.findMember(player).ifPresent(member -> {
                if (!member.player.equals(player)) {
                    return;
                }
                switch (member.membershipState) {
                    case DECLINED, PENDING -> response.add(identity, new GroupInviteResponse(group.id, group.name, group.type, null));
                    case ACCEPTED -> response.add(identity, new RejoinGroupResponse(group.id));
                    default -> throw new IllegalStateException("Unexpected value: " + member.membershipState);
                }
            });
        });
        return Optional.of(response);
    }

    /**
     * Set the player as offline
     * @param player the joining player
     * @return responses to send
     */
    public Optional<ProtocolResponse> playerIsOffline(Player player) {
        BatchProtocolResponse response = new BatchProtocolResponse();
        store.findGroupsContaining(player.identity.id()).forEach(group -> {
            PublicProfile profile = profiles.getById(player.identity.id()).orElseThrow(() -> new IllegalStateException("could not load profile " + player.identity.id())).toPublic();
            group = group.updatePlayer(new MemberSource(player, profile));
            // Let everyone else in the group know that this identity has gone offline
            Group finalGroup = group;
            BatchProtocolResponse updates = createMemberMessages(
                    group,
                    member -> member.membershipState.equals(MembershipState.ACCEPTED) && !member.player.equals(player),
                    ((memberPlayer, updatedMember) -> {
                        // As they are not playing any more, we need to clear their minecraft player and send it to all members
                        Player clearedPlayer = new Player(player.identity, null);
                        return new UpdateGroupMemberResponse(finalGroup.id, clearedPlayer, profile, Status.OFFLINE, null);
                    }));
            response.concat(updates);
            updateState(group);
        });
        return Optional.of(response);
    }

    /**
     * Accept a membership request
     *
     * @param identity
     * @param req of the new group request
     * @return response to send the client
     */
    public Optional<BatchProtocolResponse> acceptMembership(ClientIdentity identity, JoinGroupRequest req) {
        Optional<Player> sendingPlayer = sessions.findPlayer(identity);
        if (sendingPlayer.isEmpty()) {
            return Optional.empty();
        }
        return store.findGroup(req.groupId).map(group -> {
            BatchProtocolResponse response = new BatchProtocolResponse();
            MembershipState state = req.state;
            MembershipRole role = group.getRole(sendingPlayer.get());
            group = store.updateMember(group.id, sendingPlayer.get().identity.id(), role, state).orElseThrow(() -> new IllegalStateException("could not reload group " + req.groupId));
            // Let everyone in the group (including sender) know that they have accepted
            Group finalGroup = group;
            BatchProtocolResponse updates = createMemberMessages(
                    group,
                    member -> member.membershipState.equals(MembershipState.ACCEPTED),
                    ((player, updatedMember) -> new JoinGroupResponse(finalGroup, identity, player)));
            response.concat(updates);
            updateState(group);
            return response;
        });
    }

    /**
     * Leave the group
     * @param req to leave the group
     * @return response to client
     */
    public Optional<BatchProtocolResponse> leaveGroup(ClientIdentity identity, LeaveGroupRequest req) {
        Optional<Player> sender = sessions.findPlayer(identity);
        if (sender.isEmpty()) {
            return Optional.empty();
        }
        return store.findGroup(req.groupId).map(group -> {
            Group finalGroup = group;
            BatchProtocolResponse response = createMemberMessages(group, member -> true, (player, member) -> new LeaveGroupResponse(finalGroup.id, identity, sender.get()));
            group = store.removeMember(group.id, sender.get().identity.id()).orElseThrow(() -> new IllegalStateException("could not reload group " + req.groupId));
            updateState(group);
            return response;
        });
    }

    /**
     * Invite user to a group
     * @param identity caller
     * @param req request
     */
    public Optional<BatchProtocolResponse> invite(ClientIdentity identity, GroupInviteRequest req) {
        Optional<Player> player = sessions.findPlayer(identity);
        if (player.isEmpty()) {
            return Optional.empty();
        }
        return store.findGroup(req.groupId).map(group -> {
            group.members.stream()
                    .filter(member -> member.player.equals(player.get())).findFirst()
                    .filter(member -> member.membershipRole == MembershipRole.OWNER)
                    .orElseThrow(() -> new IllegalStateException("requester is not owner of group"));
            Map<Group, List<Member>> groupToMembers = new HashMap<>();
            List<MemberSource> players = sessions.findPlayers(identity, req.players).stream().map(thePlayer -> {
                PublicProfile profile = profiles.getById(thePlayer.identity.id()).orElseThrow(() -> new IllegalStateException("cannot find profile " + thePlayer.identity.id())).toPublic();
                return new MemberSource(thePlayer, profile);
            }).collect(Collectors.toList());
            // TODO: replace line below with a method that can do the diff of existing players and new players invited
            BatchProtocolResponse response = new BatchProtocolResponse();
            group = group.addMembers(players, MembershipRole.MEMBER, MembershipState.PENDING, groupToMembers::put);
            group = store.addMembers(group.id, players, MembershipRole.MEMBER, MembershipState.PENDING).orElseThrow(() -> new IllegalStateException("could not reload group " + req.groupId));
            for (Map.Entry<Group, List<Member>> entry : groupToMembers.entrySet()) {
                createGroupMembershipRequests(identity, entry.getKey(), entry.getValue()).ifPresent(response::concat);
            }
            updateState(group);
            return response;
        });
    }

    public Optional<BatchProtocolResponse> ejectMember(ClientIdentity identity, EjectGroupMemberRequest req) {
        Optional<Player> player = sessions.findPlayer(identity);
        if (player.isEmpty()) {
            return Optional.empty();
        }
        return store.findGroup(req.groupId).map(group -> {
            Optional<Member> playerMemberRecord = group.members.stream().filter(member -> member.player.equals(player.get()) && member.membershipRole.equals(MembershipRole.OWNER)).findFirst();
            if (playerMemberRecord.isEmpty()) {
                return null;
            }
            Optional<Member> memberToRemove = group.members.stream().filter(member -> member.player != null && member.player.minecraftPlayer != null && member.player.minecraftPlayer.id.equals(req.player)).findFirst();
            if (memberToRemove.isEmpty()) {
                return null;
            }
            Player playerToRemove = memberToRemove.get().player;
            Optional<ClientIdentity> identityToRemove = sessions.getIdentity(playerToRemove);
            if (identityToRemove.isEmpty()) {
                return null;
            }
            BatchProtocolResponse response = createMemberMessages(group, member -> true, (thePlayer, member) -> new LeaveGroupResponse(req.groupId, identityToRemove.get(), playerToRemove));
            group = store.removeMember(group.id, playerToRemove.identity.id()).orElseThrow(() -> new IllegalStateException("could not reload group " + req.groupId));
            updateState(group);
            return response;
        });
    }

    /**
     * Creates messages to be sent to all ACCEPTED members of the group it is addressed to
     * @param identity caller
     * @param req of the message
     * @return responses
     */
    public Optional<ProtocolResponse> createMessages(ClientIdentity identity, SendMessageRequest req) {
        Optional<Player> player = sessions.findPlayer(identity);
        if (player.isEmpty()) {
            return Optional.empty();
        }
        GroupMessageEnvelope envelope = new GroupMessageEnvelope(req.message);
        return store.findGroup(req.group).map(group -> createMemberMessages(
                group,
                member -> member.membershipState.equals(MembershipState.ACCEPTED) && !member.player.equals(player.get()) && envelope.messages.containsKey(member.player.identity.id()),
                (thePlayer, member) -> {
                    GroupMessage message = envelope.messages.get(thePlayer.identity.id());
                    Profile profile = profiles.getById(thePlayer.identity.id()).orElseThrow(() -> new IllegalStateException("could not find profile " + thePlayer.identity.id()));
                    return new SendMessageResponse(identity, group.id, thePlayer, profile.toPublic(), message.contents);
                })
        );
    }

    /**
     * Sends the group keys of the client receiving the {@link JoinGroupResponse} back to the client that joined
     * @param identity caller
     * @param req from the client receiving {@link JoinGroupResponse}
     * @return AcknowledgedGroupJoinedResponse back to the client who joined
     */
    public Optional<ProtocolResponse> acknowledgeJoin(ClientIdentity identity, AcknowledgedGroupJoinedRequest req) {
        return store.findGroup(req.group).map(group -> {
            Player player = sessions.findPlayer(identity).orElseThrow(() -> new IllegalStateException(identity + " could not be found in the session"));
            if (!group.containsPlayer(player)) {
                throw new IllegalStateException(player + " is not a member of group " + group.id);
            }
            return BatchProtocolResponse.one(req.recipient, new AcknowledgedGroupJoinedResponse(identity, player, group));
        });
    }

    public Optional<BatchProtocolResponse> updateNearbyGroups(NearbyGroups.Result result) {
        BatchProtocolResponse response = new BatchProtocolResponse();
        result.add.forEach((groupId, nearbyGroup) -> {
            Group group = new Group(groupId, null, GroupType.NEARBY, Set.of());
            Map<Group, List<Member>> groupToMembers = new HashMap<>();
            group = group.addMembers(ImmutableList.copyOf(nearbyGroup.players), MembershipRole.MEMBER, MembershipState.PENDING, groupToMembers::put);
            for (Map.Entry<Group, List<Member>> memberEntry : groupToMembers.entrySet()) {
                createGroupMembershipRequests(null, memberEntry.getKey(), memberEntry.getValue()).ifPresent(response::concat);
            }
            store.upsert(group);
        });

        // TODO: delay group removal by 1 minute
        result.remove.forEach((groupId, nearbyGroup) -> store.findGroup(groupId).ifPresent(group -> {
            for (MemberSource source : nearbyGroup.players) {
                sessions.getIdentity(source.player).ifPresent(identity -> response.add(identity, new LeaveGroupResponse(groupId, null, source.player)));
                group = group.removeMember(source.player);
            }
            store.delete(group.id);
        }));
        return response.optional();
    }

    /**
     * Sends membership requests to the group members
     * @param requester who's sending the request
     * @param group the group to invite to
     * @param members members to send requests to. If null, defaults to the full member list.
     */
    private Optional<BatchProtocolResponse> createGroupMembershipRequests(ClientIdentity requester, Group group, List<Member> members) {
        Optional<Player> sender = sessions.findPlayer(requester);
        if (sender.isEmpty() && !group.type.equals(GroupType.NEARBY)) {
            LOGGER.error("Groups of type " + group.type + " must have a sender");
            return Optional.empty();
        }
        List<Player> players = members.stream()
                .filter(member -> member.player == null || member.membershipState == MembershipState.PENDING)
                .map(member -> member.player)
                .collect(Collectors.toList());
        BatchProtocolResponse response = new BatchProtocolResponse();
        players.forEach(player -> {
            sessions.getIdentity(player).ifPresent(identity -> {
                response.add(identity, new GroupInviteResponse(group.id, group.name, group.type, sender.orElse(null)));
            });
        });
        return response.optional();
    }

    public Optional<ProtocolResponse> transferOwnership(ClientIdentity identity, TransferGroupOwnershipRequest req) {
        Optional<Player> currentPlayer = sessions.findPlayer(identity);
        if (currentPlayer.isEmpty()) {
            return Optional.empty();
        }
        return store.findGroup(req.group).map(group -> {
            UUID groupId = group.id;
            if (group.getRole(currentPlayer.get()) != MembershipRole.OWNER) {
                throw new IllegalStateException(identity + " is not owner of group " + groupId);
            }
            Member newOwner = group.members.stream()
                    .filter(member -> member.player.identity.id().equals(req.profile)).findFirst()
                    .orElseThrow(() -> new IllegalStateException(req.profile + " is not member of group " + req.group));

            if (newOwner.membershipState != MembershipState.ACCEPTED) {
                return null;
            }

            group = store.updateMember(groupId, req.profile, MembershipRole.OWNER, MembershipState.ACCEPTED).orElseThrow(() -> new IllegalStateException("cant find group " + groupId));
            // Set the member as owner
            BatchProtocolResponse removeOldOwnerMessages = createMemberMessages(
                    group,
                    member -> true,
                    (player, updatedMember) -> {
                        PublicProfile profile = profiles.getById(player.identity.id()).orElseThrow(() -> new IllegalStateException("cannot find profile " + player.identity.id())).toPublic();
                        return new UpdateGroupMemberResponse(groupId, newOwner.player, profile, player.status, MembershipRole.OWNER);
                    }
            );

            // Set original owner as member
            group = store.updateMember(groupId, currentPlayer.get().identity.id(), MembershipRole.MEMBER, MembershipState.ACCEPTED).orElseThrow(() -> new IllegalStateException("cant find group " + groupId));
            BatchProtocolResponse addNewOwnerMessages = createMemberMessages(
                    group,
                    member -> true,
                    (player, updatedMember) -> {
                        PublicProfile profile = profiles.getById(player.identity.id()).orElseThrow(() -> new IllegalStateException("cannot find profile " + player.identity.id())).toPublic();
                        return new UpdateGroupMemberResponse(groupId, currentPlayer.get(), profile, player.status, MembershipRole.MEMBER);
                    });

            return removeOldOwnerMessages.concat(addNewOwnerMessages);
        });
    }

    private void updateState(Group group) {
        if (group != null && group.members.isEmpty()) {
            LOGGER.info("Removed group " + group.id + " as it has no members.");
            store.delete(group.id);
        }
    }

    public BatchProtocolResponse createMemberMessages(Group group, Predicate<Member> filter, MessageCreator messageCreator) {
        final Map<ProtocolResponse, ClientIdentity> responses = new HashMap<>();
        for (Member member : group.members) {
            if (filter.test(member) && member.player != null) {
                ProtocolResponse resp = messageCreator.create(member.player, member);
                responses.put(resp, member.player.identity);
            }
        }
        return new BatchProtocolResponse(responses);
    }

    public Optional<Group> findGroup(UUID groupId) {
        return store.findGroup(groupId);
    }

    public AddGroupMemberResponse addGroupMember(RequestContext ctx, AddGroupMemberRequest req) {
        PublicProfile profile = findProfileAndValidateGroupMembership(ctx, req.group, GetProfileRequest.byEmail(req.byEmail));
        // Find the mc profile before we add the new group member - should 404
        Optional<PlayerProfileInfo> minecraftProfile = req.playerName == null ? Optional.empty() : mojang.api().getByName(req.playerName);
        Group group = store.addMembers(req.group, List.of(new MemberSource(null, profile)), req.membershipRole, MembershipState.ACCEPTED)
                .orElseThrow(() -> new ServerErrorException("could not update group " + req.group));
        // If we are able to find the mc profile, add it to their name
        minecraftProfile.ifPresent(mcProfile -> {
            profileService.updateProfile(RequestContext.SERVER, UpdateProfileRequest.addMinecraftAccount(profile.id, mcProfile.toId()));
        });
        return new AddGroupMemberResponse(group);
    }

    public RemoveGroupMemberResponse removeGroupMember(RequestContext ctx, RemoveGroupMemberRequest req) {
        if (req.byPlayerName != null) {
            validateCallerIsAdministratorOrOwnerOfGroup(ctx, req.group);
            PlayerProfileInfo mcProfile = mojang.api().getByName(req.byPlayerName).orElseThrow(() -> new NotFoundException("could not find player name at mojang"));
            List<Profile> profiles = profileService.getProfiles(ctx, ProfileService.GetProfilesRequest.byMinecraftId(mcProfile.toId())).profiles;
            profiles.forEach(collarProfile -> {
                store.removeMember(req.group, collarProfile.id).orElseThrow(() -> new ServerErrorException("could not update group " + req.group));
            });
        } else if (req.byEmail != null) {
            PublicProfile profile = findProfileAndValidateGroupMembership(ctx, req.group, GetProfileRequest.byEmail(req.byEmail));
            store.removeMember(req.group, profile.id).orElseThrow(() -> new ServerErrorException("could not update group " + req.group));
        } else if (req.byCollarProfileId != null) {
            PublicProfile profile = findProfileAndValidateGroupMembership(ctx, req.group, GetProfileRequest.byId(req.byCollarProfileId));
            store.removeMember(req.group, profile.id).orElseThrow(() -> new ServerErrorException("Could not remove profile: " + req.byCollarProfileId + " from group: " + req.group));
        } else {
            throw new BadRequestException("neither byPlayerName or byEmail or byCollarProfileId specified");
        }
        return new RemoveGroupMemberResponse();
    }

    private PublicProfile findProfileAndValidateGroupMembership(RequestContext ctx, UUID groupId, GetProfileRequest req) {
        PublicProfile profile = profileService.getProfile(RequestContext.SERVER, req).profile.toPublic();
        validateCallerIsAdministratorOrOwnerOfGroup(ctx, groupId);
        return profile;
    }

    private void validateCallerIsAdministratorOrOwnerOfGroup(RequestContext ctx, UUID groupId) {
        Group group = store.findGroup(groupId).orElseThrow(() -> new NotFoundException("group not found"));

        //if group type is NEARBY then nobody in the group has the OWNER rank
        if (group.type == GroupType.NEARBY)
            return;
        if (!ctx.roles.contains(Role.ADMINISTRATOR)) {
            Member caller = group.findMember(ctx.owner).orElseThrow(() -> {
                throw new ForbiddenException("caller is not a member of group " + groupId);
            });
            if (!MembershipRole.OWNER.equals(caller.membershipRole)) {
                throw new ForbiddenException("caller is not a owner member of group " + groupId);
            }
        }
    }

    public CreateGroupTokenResponse createGroupToken(RequestContext ctx, CreateGroupTokenRequest request) {
        byte[] token = store.findGroup(request.group).filter(found -> found.members.stream().anyMatch(member -> ctx.callerIs(member.profile.id)))
                .map(found -> new GroupMembershipToken(found.id, ctx.owner, Instant.now().plus(1, ChronoUnit.DAYS)))
                .map(groupMembershipToken -> {
                    try {
                        return cipher.encrypt(groupMembershipToken.serialize());
                    } catch (CipherException e) {
                        throw new ServerErrorException("token generation failed", e);
                    }
                })
                .orElseThrow(() -> new NotFoundException("group not found"));
        return new CreateGroupTokenResponse(BaseEncoding.base64Url().encode(token));
    }

    public ValidateGroupTokenResponse validateGroupToken(ValidateGroupTokenRequest req) {
        byte[] tokenBytes = BaseEncoding.base64Url().decode(req.token);
        GroupMembershipToken token;
        try {
            token = new GroupMembershipToken(cipher.decrypt(tokenBytes));
        } catch (CipherException e) {
            throw new ServerErrorException("bad token", e);
        }
        LOGGER.log(Level.INFO, "Recieved group token " + token);
        token.assertValid(req.group);
        return store.findGroup(token.group)
                .flatMap(group -> {
                    Optional<Member> member = group.findMember(token.profile);
                    member.ifPresent(value -> LOGGER.log(Level.INFO, "Found member " + value.profile.id + " in group " + group.id));
                    return member;
                })
                .flatMap(member -> {
                    Optional<Profile> profile = profiles.getById(member.profile.id);
                    profile.ifPresent(value -> LOGGER.log(Level.INFO, "Found profile for member " + value.id + " in group " + req.group));
                    return profile;
                })
                .map(profile -> createValidateGroupTokenResponse(req, profile))
                .orElseThrow(NotFoundException::new);
    }

    @Nonnull
    private ValidateGroupTokenResponse createValidateGroupTokenResponse(ValidateGroupTokenRequest req, Profile profile) {
        return new ValidateGroupTokenResponse(findGroup(req.group).map(Group::toPublicGroup).orElseThrow(NotFoundException::new), profile.toPublic());
    }

    public interface MessageCreator {
        ProtocolResponse create(Player player, Member updatedMember);
    }
}
