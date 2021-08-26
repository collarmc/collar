package com.collarmc.security.messages;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.identity.Identity;
import com.collarmc.security.CollarIdentity;

import java.util.*;

public final class GroupSession {
    private final UUID group;
    private final CollarIdentity collarIdentity;
    private final Cipher cipher;
    private final IdentityStore<ClientIdentity> store;
    private final Set<ClientIdentity> recipients;

    public GroupSession(UUID group, IdentityStore<ClientIdentity> store, CollarIdentity collarIdentity, Set<ClientIdentity> recipients) {
        this.group = group;
        this.collarIdentity = collarIdentity;
        this.cipher = new Cipher(collarIdentity);
        this.store = store;
        this.recipients = recipients;
    }

    public byte[] encrypt(byte[] plainText) throws CipherException {
        List<GroupMessage> messages = new ArrayList<>();
        for (Identity recipient : recipients) {
            // Don't send a message to myself
            if (recipient.id().equals(store.identity().id())) {
                continue;
            }
            byte[] cipherText = cipher.encrypt(plainText, recipient);
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
        return cipher.decrypt(cipherText, sender);
    }

    public GroupSession add(ClientIdentity identity) {
        Set<ClientIdentity> identities = new HashSet<>(recipients);
        identities.add(identity);
        return new GroupSession(group, store, collarIdentity, identities);
    }

    public GroupSession remove(ClientIdentity identity) {
        Set<ClientIdentity> identities = new HashSet<>(recipients);
        identities.remove(identity);
        return new GroupSession(group, store, collarIdentity, identities);
    }
}
