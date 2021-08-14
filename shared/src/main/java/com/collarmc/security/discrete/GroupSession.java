package com.collarmc.security.discrete;

import com.collarmc.io.IO;
import com.collarmc.security.ClientIdentity;
import com.collarmc.security.Identity;
import com.collarmc.utils.Utils;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.crypto.tink.KeysetHandle;

import java.util.*;
import java.util.stream.Collectors;

public final class GroupSession {
    private final UUID group;
    private final Cipher cipher;
    private final IdentityStore<ClientIdentity> store;
    private final Set<ClientIdentity> recipients;

    public GroupSession(Identity self, UUID group, IdentityStore<ClientIdentity> store, KeysetHandle messagingPublicKey, KeysetHandle identityPublicKey, Set<ClientIdentity> recipients) {
        this.group = group;
        this.cipher = new Cipher(self, store, messagingPublicKey, identityPublicKey);
        this.store = store;
        this.recipients = recipients;
    }

    public GroupSession(Identity self, UUID group, IdentityStore<ClientIdentity> store, Cipher cipher, Set<ClientIdentity> recipients) {
        this.group = group;
        this.cipher = cipher;
        this.store = store;
        this.recipients = recipients;
    }

    public byte[] encrypt(byte[] plainText) throws CipherException {
        byte[] sessionId = createSessionId(group, recipients);
        List<GroupMessageEnvelope> envelopes = new ArrayList<>();
        for (Identity recipient : recipients) {
            // Don't send a message to myself
            if (recipient.id().equals(store.identity().id())) {
                continue;
            }
            byte[] cipherText = cipher.encrypt(plainText, recipient, sessionId);
            envelopes.add(new GroupMessageEnvelope(recipient, new GroupMessage(group, sessionId, cipherText).serialize()));
        }
        try {
            return Utils.messagePackMapper().writeValueAsBytes(envelopes);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public GroupMessage decrypt(byte[] cipherText, Identity sender) throws CipherException {
        return new GroupMessage(cipher.decrypt(cipherText, sender, createSessionId(group, recipients)));
    }

    /**
     * Creates a session id using a sha256 of the groupId and recipient id's
     * @param groupId of group chat
     * @param members of group
     * @return sessionID
     */
    static byte[] createSessionId(UUID groupId, Set<ClientIdentity> members) {
        Hasher hasher = Hashing.sha256().newHasher();
        List<UUID> recipientIds = members.stream().map(identity -> identity.id()).collect(Collectors.toList());
        recipientIds = new ArrayList<>(recipientIds);
        recipientIds.add(groupId);
        recipientIds.stream().sorted(Comparator.comparing(o -> o)).forEach(id -> hasher.putBytes(IO.writeUUIDToBytes(id)));
        return hasher.hash().asBytes();
    }

    public GroupSession add(ClientIdentity identity) {
        Set<ClientIdentity> identities = new HashSet<>(recipients);
        identities.add(identity);
        return new GroupSession(identity, group, store, cipher, identities);
    }

    public GroupSession remove(ClientIdentity identity) {
        Set<ClientIdentity> identities = new HashSet<>(recipients);
        identities.remove(identity);
        return new GroupSession(identity, group, store, cipher, identities);
    }
}
