package com.collarmc.security.discrete;

import com.collarmc.security.ClientIdentity;
import com.collarmc.security.Identity;
import com.collarmc.security.PublicKey;
import com.google.crypto.tink.*;
import com.google.crypto.tink.hybrid.HybridConfig;
import com.google.crypto.tink.signature.SignatureConfig;
import com.google.crypto.tink.signature.SignatureKeyTemplates;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class IdentityStoreImpl implements IdentityStore<ClientIdentity> {

    private final ConcurrentMap<UUID, Identity> identities = new ConcurrentHashMap<>();

    private final UUID id = UUID.randomUUID();
    final KeysetHandle messagingPrivateKey;
    final KeysetHandle messagingPublicKey;

    final KeysetHandle identityPrivateKey;
    final KeysetHandle identityPublicKey;

    public IdentityStoreImpl() {
        try {
            messagingPrivateKey = KeysetHandle.generateNew(KeyTemplates.get("ECIES_P256_HKDF_HMAC_SHA256_AES128_CTR_HMAC_SHA256"));
            messagingPublicKey = messagingPrivateKey.getPublicKeysetHandle();

            identityPrivateKey = KeysetHandle.generateNew(SignatureKeyTemplates.ED25519);
            identityPublicKey = identityPrivateKey.getPublicKeysetHandle();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void trustIdentity(Identity identity) {
        identities.put(identity.id(), identity);
    }

    @Override
    public boolean isTrustedIdentity(Identity identity) {
        return false;
    }

    @Override
    public Cipher cipher() {
        return null;
    }

    @Override
    public ClientIdentity identity() {
        return new ClientIdentity(id, new PublicKey(serializeKey(identityPublicKey)));
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

    static {
        try {
            HybridConfig.register();
            SignatureConfig.register();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
