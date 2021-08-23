package com.collarmc.client.security;

import com.collarmc.api.groups.Group;
import com.collarmc.api.groups.MembershipState;
import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.identity.Identity;
import com.collarmc.api.identity.ServerIdentity;
import com.collarmc.client.HomeDirectory;
import com.collarmc.protocol.devices.DeviceRegisteredResponse;
import com.collarmc.protocol.groups.*;
import com.collarmc.protocol.identity.CreateTrustRequest;
import com.collarmc.protocol.identity.IdentifyRequest;
import com.collarmc.protocol.identity.IdentifyResponse;
import com.collarmc.security.CollarIdentity;
import com.collarmc.security.PublicKey;
import com.collarmc.security.TokenGenerator;
import com.collarmc.security.messages.Cipher;
import com.collarmc.security.messages.CipherException;
import com.collarmc.security.messages.GroupSession;
import com.collarmc.utils.Utils;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class ClientIdentityStoreImpl implements ClientIdentityStore {

    private static final Logger LOGGER = LogManager.getLogger(ClientIdentityStoreImpl.class);
    private final GroupSessionManager groupSessionManager = new GroupSessionManager(this);
    private final CollarIdentity collarIdentity;

    private State state;
    private final File file;
    private final byte[] token = TokenGenerator.byteToken(256);

    public ClientIdentityStoreImpl(HomeDirectory directory) throws IOException, CipherException {
        this.collarIdentity = CollarIdentity.getOrCreate(directory.profile());
        this.file = filePath(directory);
        if (file.exists()) {
            state = Utils.messagePackMapper().readValue(file, State.class);
        } else {
            state = new State(null, new ConcurrentHashMap<>(), null);
            save();
        }
    }

    @Override
    public ClientIdentity identity() {
        if (state.profile == null) {
            throw new IllegalStateException("profile not set");
        }
        return new ClientIdentity(state.profile, collarIdentity.publicKey(), collarIdentity.signatureKey());
    }

    @Override
    public ServerIdentity serverIdentity() {
        return state.serverIdentity;
    }

    @Override
    public IdentifyRequest createIdentifyRequest() {
        if (isValid()) {
            try {
                return new IdentifyRequest(identity(), cipher().encrypt(token, state.serverIdentity));
            } catch (CipherException e) {
                LOGGER.log(Level.ERROR, "could not encrypt token", e);
                throw new RuntimeException(e);
            }
        } else {
            return IdentifyRequest.unknown();
        }
    }

    @Override
    public boolean verifyIdentityResponse(IdentifyResponse response) {
        // Decrypt the token received from the server
        byte[] token;
        try {
            ServerIdentity serverIdentity = serverIdentity();
            token = cipher().decrypt(response.token, serverIdentity.id(), serverIdentity.signatureKey());
        } catch (CipherException e) {
            LOGGER.log(Level.ERROR, "could not decrypt token", e);
            return false;
        }
        // Check that the token from server matches the token we initially sent
        return Arrays.equals(this.token, token);
    }

    @Override
    public GroupSession createSession(Group group) {
        return new GroupSession(identity(), group.id, this, collarIdentity, group.members.stream().map(member -> member.player.identity).collect(Collectors.toSet()));
    }

    @Override
    public GroupSessionManager groupSessions() {
        return groupSessionManager;
    }

    @Override
    public boolean isTrustedIdentity(Identity identity) {
        PublicKey publicKey = state.keys.get(identity.id());
        return publicKey != null && Arrays.equals(identity.publicKey().key, publicKey.key);
    }

    @Override
    public void trustIdentity(Identity identity) {
        state.keys.compute(identity.id(), (uuid, publicKey) -> identity.publicKey());
        try {
            save();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Cipher<ClientIdentity> cipher() {
        return new Cipher<>(identity(), this, collarIdentity);
    }

    @Override
    public IdentifyRequest processDeviceRegisteredResponse(DeviceRegisteredResponse response) {
        trustIdentity(response.serverIdentity);
        state = new State(response.profile.id, state.keys, response.serverIdentity);
        try {
            save();
        } catch (IOException e) {
            throw new IllegalStateException("state update failed", e);
        }
        return createIdentifyRequest();
    }

    @Override
    public JoinGroupRequest createJoinGroupRequest(UUID groupId) {
        return new JoinGroupRequest(groupId, MembershipState.ACCEPTED);
    }

    @Override
    public CreateTrustRequest createCreateTrustRequest(ClientIdentity recipient, long id) {
        return new CreateTrustRequest(id, recipient);
    }

    @Override
    public AcknowledgedGroupJoinedRequest processJoinGroupResponse(JoinGroupResponse resp) {
        groupSessionManager.create(resp.group);
        resp.group.members.stream().map(member -> member.player.identity).forEach(this::trustIdentity);
        return new AcknowledgedGroupJoinedRequest(resp.sender, resp.group.id);
    }

    @Override
    public void processAcknowledgedGroupJoinedResponse(AcknowledgedGroupJoinedResponse response) {
        // Trust members
        response.group.members.stream().map(member -> member.player.identity).forEach(this::trustIdentity);
        groupSessionManager.create(response.group);
    }

    @Override
    public void processLeaveGroupResponse(LeaveGroupResponse response) {
        groupSessionManager.delete(response.groupId);
    }

    @Override
    public void clearAllGroupSessions() {
        groupSessionManager.clear();
    }

    @Override
    public void reset() throws IOException {
        state = new State(null, new ConcurrentHashMap<>(), null);
        save();
    }

    @Override
    public void save() throws IOException {
        Utils.messagePackMapper().writeValue(file, state);
    }

    public boolean isValid() {
        return state != null && state.profile != null && state.serverIdentity != null;
    }

    private static File filePath(HomeDirectory directory) throws IOException {
        return new File(directory.security(), "keystore");
    }

    private static class State {

        @JsonProperty("profile")
        public final UUID profile;

        @JsonProperty("keys")
        public final ConcurrentMap<UUID, PublicKey> keys;

        public final ServerIdentity serverIdentity;

        public State(@JsonProperty("profile") UUID profile,
                     @JsonProperty("keys") ConcurrentMap<UUID, PublicKey> keys,
                     @JsonProperty("serverIdentity") ServerIdentity serverIdentity) {
            this.profile = profile;
            this.keys = keys;
            this.serverIdentity = serverIdentity;
        }
    }
}
