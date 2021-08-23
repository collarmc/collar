package com.collarmc.server.protocol;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.session.Player;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.protocol.groups.*;
import com.collarmc.server.CollarServer;
import com.collarmc.server.Services;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;

import java.util.Optional;
import java.util.function.BiConsumer;

public final class GroupsProtocolHandler extends ProtocolHandler {
    private static final Logger LOGGER = LogManager.getLogger(GroupsProtocolHandler.class.getName());

    private final Services services;

    public GroupsProtocolHandler(Services services) {
        this.services = services;
    }

    @Override
    public boolean handleRequest(CollarServer collar, ClientIdentity identity, ProtocolRequest req, BiConsumer<ClientIdentity, ProtocolResponse> sender) {
        Optional<? extends ProtocolResponse> resp;
        if (req instanceof CreateGroupRequest) {
            CreateGroupRequest request = (CreateGroupRequest)req;
            resp = services.groups.createGroup(identity, request);
        } else if (req instanceof JoinGroupRequest) {
            JoinGroupRequest request = (JoinGroupRequest) req;
            resp = services.groups.acceptMembership(identity, request);
        } else if (req instanceof LeaveGroupRequest) {
            LeaveGroupRequest request = (LeaveGroupRequest)req;
            resp = services.groups.leaveGroup(identity, request);
        } else if (req instanceof GroupInviteRequest) {
            GroupInviteRequest request = (GroupInviteRequest)req;
            resp = services.groups.invite(identity, request);
        } else if (req instanceof EjectGroupMemberRequest) {
            EjectGroupMemberRequest request = (EjectGroupMemberRequest) req;
            resp = services.groups.ejectMember(identity, request);
        } else if (req instanceof AcknowledgedGroupJoinedRequest) {
            AcknowledgedGroupJoinedRequest request = (AcknowledgedGroupJoinedRequest) req;
            resp = services.groups.acknowledgeJoin(identity, request);
        } else if (req instanceof DeleteGroupRequest) {
            DeleteGroupRequest request = (DeleteGroupRequest) req;
            resp = services.groups.delete(identity, request);
        } else if (req instanceof TransferGroupOwnershipRequest) {
            TransferGroupOwnershipRequest request = (TransferGroupOwnershipRequest) req;
            resp = services.groups.transferOwnership(identity, request);
        } else {
            resp = Optional.empty();
        }
        resp.ifPresent(protocolResponse -> {
            sender.accept(identity, protocolResponse);
        });
        return resp.isPresent();
    }

    @Override
    public void onSessionStarted(ClientIdentity identity, Player player, BiConsumer<Session, ProtocolResponse> sender) {
        super.onSessionStarted(identity, player, sender);
        services.groups.playerIsOnline(identity, player).ifPresent(response -> sender.accept(null, response));
    }

    @Override
    public void onSessionStopping(ClientIdentity identity, Player player, BiConsumer<Session, ProtocolResponse> sender) {
        super.onSessionStopping(identity, player, sender);
        if (player == null) return;
        services.groups.playerIsOffline(player).ifPresent(response -> sender.accept(null, response));
    }
}
