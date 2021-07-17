package com.collarmc.client.sdht.cipher;

import com.collarmc.client.security.ClientIdentityStore;
import com.collarmc.api.groups.Group;
import com.collarmc.client.api.groups.GroupsApi;
import com.collarmc.sdht.Content;
import com.collarmc.sdht.cipher.ContentCipher;
import com.collarmc.security.ClientIdentity;
import com.collarmc.security.cipher.CipherException;
import com.collarmc.security.cipher.CipherException.UnavailableCipherException;

import java.util.UUID;
import java.util.function.Supplier;

public final class GroupContentCipher implements ContentCipher {

    private final GroupsApi groupsApi;
    private final Supplier<ClientIdentityStore> identityStoreSupplier;

    public GroupContentCipher(GroupsApi groupsApi, Supplier<ClientIdentityStore> identityStoreSupplier) {
        this.groupsApi = groupsApi;
        this.identityStoreSupplier = identityStoreSupplier;
    }

    @Override
    public byte[] crypt(ClientIdentity identity, UUID namespace, Content content) throws CipherException {
        Group group = groupsApi.findGroupById(namespace).orElseThrow(() -> new UnavailableCipherException("could not find group " + namespace));
        return identityStoreSupplier.get().createCypher().crypt(identity, group, content.serialize());
    }

    @Override
    public Content decrypt(ClientIdentity identity, UUID namespace, byte[] bytes) throws CipherException {
        Group group = groupsApi.findGroupById(namespace).orElseThrow(() -> new UnavailableCipherException("could not find group " + namespace));
        byte[] decryptedBytes = identityStoreSupplier.get().createCypher().decrypt(identity, group, bytes);
        return new Content(decryptedBytes);
    }

    @Override
    public boolean accepts(UUID namespace) {
        return groupsApi.findGroupById(namespace).isPresent();
    }
}
