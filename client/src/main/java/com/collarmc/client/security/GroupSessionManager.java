package com.collarmc.client.security;

import com.collarmc.api.groups.Group;
import com.collarmc.api.session.Player;
import com.collarmc.security.discrete.GroupSession;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class GroupSessionManager {

    private final ConcurrentMap<UUID, GroupSession> sessions = new ConcurrentHashMap<>();
    private final ClientIdentityStore store;

    public GroupSessionManager(ClientIdentityStore store) {
        this.store = store;
    }

    public GroupSession session(Group group) {
        return sessions.get(group.id);
    }

    public GroupSession create(Group group) {
        return sessions.compute(group.id, (uuid, groupSession) -> store.createSession(group));
    }

    public GroupSession delete(Group group) {
        return sessions.remove(group.id);
    }

    public GroupSession addPlayer(Group group, Player player) {
        return sessions.compute(group.id, (uuid, groupSession) -> {
            if (groupSession == null) {
                throw new IllegalStateException("no session for group " + group.id);
            }
            return groupSession.add(player.identity);
        });
    }

    public GroupSession removePlayer(Group group, Player player) {
        return sessions.compute(group.id, (uuid, groupSession) -> {
            if (groupSession == null) {
                throw new IllegalStateException("no session for group " + group.id);
            }
            return groupSession.remove(player.identity);
        });
    }
}
