package com.collarmc.security.discrete;

import com.collarmc.io.IO;
import com.collarmc.security.Identity;
import com.collarmc.security.discrete.CipherException.UnknownCipherException;
import com.google.crypto.tink.*;

import java.io.IOException;
import java.security.GeneralSecurityException;

public final class Cipher<T extends Identity> {

    private final Identity self;
    private final IdentityStore<T> identityStore;
    private final KeysetHandle messagingPrivateKey;

    private final KeysetHandle identityPrivateKey;

    public Cipher(Identity self,
                  IdentityStore<T> identityStore,
                  KeysetHandle messagingPrivateKey,
                  KeysetHandle identityPrivateKey) {
        this.self = self;
        this.identityStore = identityStore;
        this.messagingPrivateKey = messagingPrivateKey;
        this.identityPrivateKey = identityPrivateKey;
    }

    public byte[] encrypt(byte[] plain, Identity receiver) throws CipherException {
        return encrypt(plain, receiver, IO.writeUUIDToBytes(self.id()));
    }

    byte[] encrypt(byte[] plain, Identity receiver, byte[] contextInfo) throws CipherException {
        try {
            HybridEncrypt hybridEncrypt = KeysetHandle.readNoSecret(receiver.publicKey().key).getPrimitive(HybridEncrypt.class);
            PublicKeySign sign = identityPrivateKey.getPrimitive(PublicKeySign.class);
            MessageEnvelope messageEnvelope = new MessageEnvelope(sign.sign(plain), plain);
            return hybridEncrypt.encrypt(messageEnvelope.serialize(), contextInfo);
        } catch (GeneralSecurityException e) {
            throw new UnknownCipherException("failed to encrypt data", e);
        }
    }

    public byte[] decrypt(byte[] content, Identity sender) throws CipherException {
        return decrypt(content, sender, IO.writeUUIDToBytes(sender.id()));
    }

    byte[] decrypt(byte[] content, Identity sender, byte[] contextInfo) throws CipherException {
        if (identityStore.isTrustedIdentity(sender)) {
            try {
                HybridDecrypt hybridDecrypt = messagingPrivateKey.getPrimitive(HybridDecrypt.class);
                byte[] bytes = hybridDecrypt.decrypt(content, contextInfo);
                MessageEnvelope messageEnvelope = new MessageEnvelope(bytes);
                KeysetHandle handle = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(sender.publicKey().key));
                PublicKeyVerify verify = handle.getPrimitive(PublicKeyVerify.class);
                verify.verify(messageEnvelope.signature, messageEnvelope.contents);
                return messageEnvelope.contents;
            } catch (IOException | GeneralSecurityException e) {
                throw new UnknownCipherException("failed to decrypt data", e);
            }
        } else {
            throw new UnknownCipherException("untrusted identity " + sender);
        }
    }

    /**
     * Encrypt for self storage
     * @param bytes to encrypt
     * @return cipher text
     */
    public byte[] encrypt(byte[] bytes) throws CipherException {
        throw new IllegalStateException("not implemented");
    }

    /**
     * Decrypt for self storage
     * @param bytes to encrypt
     * @return cipher text
     */
    public byte[] decrypt(byte[] bytes) throws CipherException {
        throw new IllegalStateException("not implemented");
    }
}
