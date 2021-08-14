package com.collarmc.client.security;

import com.collarmc.api.groups.Group;
import com.collarmc.client.HomeDirectory;
import com.collarmc.protocol.devices.DeviceRegisteredResponse;
import com.collarmc.protocol.groups.*;
import com.collarmc.protocol.identity.CreateTrustRequest;
import com.collarmc.security.ClientIdentity;
import com.collarmc.security.Identity;
import com.collarmc.security.PublicKey;
import com.collarmc.security.discrete.Cipher;
import com.collarmc.security.discrete.CipherException.UnknownCipherException;
import com.collarmc.security.discrete.GroupSession;
import com.collarmc.utils.Utils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.crypto.tink.*;
import com.google.crypto.tink.signature.SignatureKeyTemplates;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class ClientIdentityStoreImpl implements ClientIdentityStore {

    private static final int VERSION = 1;
    private final GroupSessionManager groupSessionManager = new GroupSessionManager(this);

    final KeysetHandle messagingPrivateKey;
    final KeysetHandle messagingPublicKey;

    final KeysetHandle identityPrivateKey;
    final KeysetHandle identityPublicKey;

    private State state;
    private final File file;

    public ClientIdentityStoreImpl(HomeDirectory directory) throws IOException, UnknownCipherException {
        this.file = filePath(directory);
        if (file.exists()) {
            state = Utils.messagePackMapper().readValue(file, State.class);
            try {
                messagingPrivateKey = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(state.messagingPrivateKey));
                messagingPublicKey = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(state.messagingPublicKey));
                identityPrivateKey = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(state.identityPrivateKey));
                identityPublicKey = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(state.identityPublicKey));
            } catch (GeneralSecurityException e) {
                throw new UnknownCipherException("could not read keys", e);
            }
        } else {
            try {
                messagingPrivateKey = KeysetHandle.generateNew(KeyTemplates.get("ECIES_P256_HKDF_HMAC_SHA256_AES128_CTR_HMAC_SHA256"));
                messagingPublicKey = messagingPrivateKey.getPublicKeysetHandle();
                identityPrivateKey = KeysetHandle.generateNew(SignatureKeyTemplates.ED25519);
                identityPublicKey = identityPrivateKey.getPublicKeysetHandle();
            } catch (GeneralSecurityException e) {
                throw new UnknownCipherException("could not create keys", e);
            }
            state = new State(null, serializeKey(messagingPrivateKey), serializeKey(messagingPublicKey), serializeKey(identityPrivateKey), serializeKey(identityPublicKey), new ConcurrentHashMap<>());
            save();
        }
    }

    @Override
    public ClientIdentity identity() {
        if (state.profile == null) {
            throw new IllegalStateException("profile not set");
        }
        return new ClientIdentity(null, new PublicKey(serializeKey(identityPublicKey)));
    }

    @Override
    public GroupSession createSession(Group group) {
        return new GroupSession(identity(), group.id, this, messagingPublicKey, identityPrivateKey, group.members.stream().map(member -> member.player.identity).collect(Collectors.toSet()));
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
    }

    @Override
    public Cipher<ClientIdentity> cipher() {
        return new Cipher<>(identity(), this, messagingPrivateKey, identityPrivateKey);
    }

    @Override
    public void processDeviceRegisteredResponse(DeviceRegisteredResponse response) {
        state = new State(response.profile.id, state.messagingPrivateKey, state.messagingPublicKey, state.identityPrivateKey, state.identityPublicKey, state.keys);
        try {
            save();
        } catch (IOException e) {
            throw new IllegalStateException("state update failed", e);
        }
    }

    @Override
    public JoinGroupRequest createJoinGroupRequest(ClientIdentity identity, UUID groupId) {
        return null;
    }

    @Override
    public CreateTrustRequest createPreKeyRequest(ClientIdentity identity, long id) {
        return null;
    }

    @Override
    public AcknowledgedGroupJoinedRequest processJoinGroupResponse(JoinGroupResponse resp) {
        return null;
    }

    @Override
    public void processAcknowledgedGroupJoinedResponse(AcknowledgedGroupJoinedResponse response) {

    }

    @Override
    public void processLeaveGroupResponse(LeaveGroupResponse response) {

    }

    @Override
    public void clearAllGroupSessions() {

    }

    @Override
    public void reset() throws IOException {
    }

    @Override
    public void save() throws IOException {
        Utils.messagePackMapper().writeValue(file, state);
    }

    private static byte[] serializeKey(KeysetHandle handle) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            CleartextKeysetHandle.write(handle, BinaryKeysetWriter.withOutputStream(bos));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return bos.toByteArray();
    }

    public static boolean exists(HomeDirectory homeDirectory) {
        try {
            return filePath(homeDirectory).exists();
        } catch (IOException e) {
            return false;
        }
    }

    private static File filePath(HomeDirectory directory) throws IOException {
        return new File(directory.security(), "keystore");
    }

    private static class State {

        @JsonProperty("profile")
        public final UUID profile;

        @JsonProperty("messagingPrivateKey")
        public final byte[] messagingPrivateKey;

        @JsonProperty("messagingPublicKey")
        public final byte[] messagingPublicKey;

        @JsonProperty("identityPrivateKey")
        public final byte[] identityPrivateKey;

        @JsonProperty("identityPublicKey")
        public final byte[] identityPublicKey;

        @JsonProperty("keys")
        public final ConcurrentMap<UUID, PublicKey> keys;

        public State(@JsonProperty("profile") UUID profile,
                     @JsonProperty("messagingPrivateKey") byte[] messagingPrivateKey,
                     @JsonProperty("messagingPublicKey") byte[] messagingPublicKey,
                     @JsonProperty("identityPrivateKey") byte[] identityPrivateKey,
                     @JsonProperty("identityPublicKey") byte[] identityPublicKey,
                     @JsonProperty("keys") ConcurrentMap<UUID, PublicKey> keys) {
            this.profile = profile;
            this.messagingPrivateKey = messagingPrivateKey;
            this.messagingPublicKey = messagingPublicKey;
            this.identityPrivateKey = identityPrivateKey;
            this.identityPublicKey = identityPublicKey;
            this.keys = keys;
        }
    }
}
