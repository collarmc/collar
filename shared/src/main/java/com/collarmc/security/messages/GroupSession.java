package com.collarmc.security.messages;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.identity.Identity;
import com.collarmc.io.IO;
import com.collarmc.security.CollarIdentity;
import com.collarmc.security.messages.CipherException.UnknownCipherException;
import com.collarmc.utils.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import java.util.*;
import java.util.stream.Collectors;

public final class GroupSession {
    private final UUID group;
    private final CollarIdentity collarIdentity;
    private final Cipher<ClientIdentity> cipher;
    private final IdentityStore<ClientIdentity> store;
    private final Set<ClientIdentity> recipients;

    public GroupSession(Identity self, UUID group, IdentityStore<ClientIdentity> store, CollarIdentity collarIdentity, Set<ClientIdentity> recipients) {
        this.group = group;
        this.collarIdentity = collarIdentity;
        this.cipher = new Cipher<>(self, store, collarIdentity);
        this.store = store;
        this.recipients = recipients;
    }

    public byte[] encrypt(byte[] plainText) throws CipherException {
        byte[] sessionId = createSessionId(group, recipients);
        List<GroupMessage> messages = new ArrayList<>();
        for (Identity recipient : recipients) {
            // Don't send a message to myself
            if (recipient.id().equals(store.identity().id())) {
                continue;
            }
            byte[] cipherText = cipher.encrypt(plainText, recipient, sessionId);
            messages.add(new GroupMessage(recipient.id(), cipherText));
        }
        return new GroupMessageEnvelope(messages).serialize();
    }

    /**
     * @param cipherText value of {@link GroupMessage#contents}
     * @param sender of the message
     * @return plain text
     * @throws CipherException if error occurs
     */
    public byte[] decrypt(byte[] cipherText, Identity sender) throws CipherException {
        return cipher.decrypt(cipherText, sender, createSessionId(group, recipients));
    }

    /**
     * Creates a session id using a sha256 of the groupId and recipient id's
     * @param groupId of group chat
     * @param members of group
     * @return sessionID
     */
    static byte[] createSessionId(UUID groupId, Set<ClientIdentity> members) {
        Hasher hasher = Hashing.sha256().newHasher();
        List<UUID> recipientIds = members.stream().map(ClientIdentity::id).collect(Collectors.toList());
        recipientIds = new ArrayList<>(recipientIds);
        recipientIds.add(groupId);
        recipientIds.stream().sorted(Comparator.comparing(o -> o)).forEach(id -> hasher.putBytes(IO.writeUUIDToBytes(id)));
        return hasher.hash().asBytes();
    }

    public GroupSession add(ClientIdentity identity) {
        Set<ClientIdentity> identities = new HashSet<>(recipients);
        identities.add(identity);
        return new GroupSession(identity, group, store, collarIdentity, identities);
    }

    public GroupSession remove(ClientIdentity identity) {
        Set<ClientIdentity> identities = new HashSet<>(recipients);
        identities.remove(identity);
        return new GroupSession(identity, group, store, collarIdentity, identities);
    }
}
