package com.collarmc.client.security;

import com.collarmc.api.groups.Group;
import com.collarmc.security.messages.GroupSession;

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

    public Optional<GroupSession> session(Group group) {
        return Optional.ofNullable(sessions.get(group.id));
    }

    public GroupSession createOrUpdate(Group group) {
        return sessions.compute(group.id, (uuid, groupSession) -> store.createSession(group));
    }

    public GroupSession delete(UUID group) {
        return sessions.remove(group);
    }

    public void clear() {
        sessions.clear();
    }
}
