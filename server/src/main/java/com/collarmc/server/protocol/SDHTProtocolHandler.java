package com.collarmc.server.protocol;

import com.collarmc.api.groups.Group;
import com.collarmc.api.groups.Member;
import com.collarmc.api.groups.MembershipState;
import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.session.Player;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.protocol.sdht.SDHTEventRequest;
import com.collarmc.protocol.sdht.SDHTEventResponse;
import com.collarmc.sdht.events.*;
import com.collarmc.security.messages.GroupMessage;
import com.collarmc.security.messages.GroupMessageEnvelope;
import com.collarmc.server.CollarServer;
import com.collarmc.server.Services;
import org.eclipse.jetty.websocket.api.Session;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

public class SDHTProtocolHandler extends ProtocolHandler {

    public SDHTProtocolHandler(Services services) {
        super(services);
    }

    @Override
    public boolean handleRequest(CollarServer collar, ClientIdentity identity, ProtocolRequest req, BiConsumer<ClientIdentity, ProtocolResponse> sender) {
        if (req instanceof SDHTEventRequest) {
            SDHTEventRequest request = (SDHTEventRequest) req;
            AbstractSDHTEvent e = request.event;
            if (e instanceof CreateEntryEvent) {
                CreateEntryEvent event = (CreateEntryEvent) e;
                GroupMessageEnvelope envelope = new GroupMessageEnvelope(event.content);
                findListeners(identity, event.record.key.namespace).forEach(found -> {
                    GroupMessage message = envelope.messages.get(identity.id());
                    if (message != null) {
                        CreateEntryEvent newEvent = new CreateEntryEvent(identity, null, event.record, message.contents);
                        SDHTEventResponse response = new SDHTEventResponse(newEvent);
                        sender.accept(found, response);
                    }
                });
                return true;
            } else if (e instanceof DeleteRecordEvent) {
                DeleteRecordEvent event = (DeleteRecordEvent) e;
                findListeners(identity, event.delete.key.namespace).forEach(found -> {
                    SDHTEventResponse response = new SDHTEventResponse(event);
                    sender.accept(found, response);
                });
            } else if (e instanceof PublishRecordsEvent) {
                PublishRecordsEvent event = (PublishRecordsEvent) e;
                SDHTEventResponse response = new SDHTEventResponse(event);
                sender.accept(event.recipient, response);
            } else if (e instanceof SyncRecordsEvent) {
                SyncRecordsEvent event = (SyncRecordsEvent) e;
                SDHTEventResponse response = new SDHTEventResponse(new SyncRecordsEvent(identity, event.namespace));
                findListeners(identity, event.namespace).forEach(found -> {
                    sender.accept(found, response);
                });
            } else if (e instanceof SyncContentEvent) {
                SyncContentEvent event = (SyncContentEvent) e;
                SDHTEventResponse response = new SDHTEventResponse(new SyncContentEvent(identity, event.recipient, event.record));
                sender.accept(event.recipient, response);
            }
            return true;
        }
        return false;
    }

    private Set<ClientIdentity> findListeners(ClientIdentity sender, UUID namespace) {
        Group group = services.groups.findGroup(namespace).orElseThrow(() -> new IllegalStateException("could not find group " + namespace));
        Optional<Player> sendingPlayer = services.sessions.findPlayer(sender);
        if (sendingPlayer.isEmpty()) {
            return Set.of();
        }
        if (!group.containsPlayer(sendingPlayer.get())) {
            throw new IllegalStateException("sender is not a member of group " + namespace);
        }
        Set<ClientIdentity> listeners = new HashSet<>();
        for (Member member : group.members) {
            if (member.membershipState != MembershipState.ACCEPTED && sendingPlayer.get().equals(member.player)) {
                continue;
            }
            services.sessions.getIdentity(member.player).ifPresent(listeners::add);
        }
        return listeners;
    }

    @Override
    public void onSessionStopping(ClientIdentity identity, Player player, BiConsumer<Session, ProtocolResponse> sender) { }
}
