package com.collarmc.security.messages;

import com.collarmc.api.identity.Identity;
import com.collarmc.io.IO;
import com.collarmc.security.CollarIdentity;
import com.collarmc.security.PublicKey;
import com.collarmc.security.messages.CipherException.UnknownCipherException;
import com.google.crypto.tink.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.UUID;

/**
 * Message Cipher service for encrypting and decrypting protocol messages
 * All messages processed with this service are encrypted and signed
 * @param <T> identity type
 */
public final class Cipher<T extends Identity> {

    private final Identity self;
    private final IdentityStore<T> identityStore;
    private final CollarIdentity identity;

    public Cipher(Identity self,
                  IdentityStore<T> identityStore,
                  CollarIdentity identity) {
        this.self = self;
        this.identityStore = identityStore;
        this.identity = identity;
    }

    /**
     * Encrypts data for an intended recipient
     * @param plain to encrypt
     * @param recipient receiving the message
     * @return cipher text
     * @throws CipherException if encryption fails
     */
    public byte[] encrypt(byte[] plain, Identity recipient) throws CipherException {
        return encrypt(plain, recipient, IO.writeUUIDToBytes(self.id()));
    }

    /**
     * Encrypts data for an intended recipient
     * @param plain to encrypt
     * @param recipient receiving the message
     * @return cipher text
     * @throws CipherException if encryption fails
     */
    byte[] encrypt(byte[] plain, Identity recipient, byte[] contextInfo) throws CipherException {
        try {
            HybridEncrypt hybridEncrypt = KeysetHandle.readNoSecret(recipient.publicKey().key).getPrimitive(HybridEncrypt.class);
            PublicKeySign sign = identity.signatureKey.getPrimitive(PublicKeySign.class);
            byte[] signature = sign.sign(plain);
            SignedMessage signedMessage = new SignedMessage(signature, plain);
            return hybridEncrypt.encrypt(signedMessage.serialize(), contextInfo);
        } catch (GeneralSecurityException e) {
            throw new UnknownCipherException("failed to encrypt data", e);
        }
    }

    /**
     * Decrypts message with trust verification
     * @param content to decrypt
     * @param sender identity
     * @return plain text
     * @throws CipherException if decryption fails
     */
    public byte[] decrypt(byte[] content, Identity sender) throws CipherException {
        return decrypt(content, sender, IO.writeUUIDToBytes(sender.id()));
    }

    /**
     * Decrypts message
     * @param content to decrypt
     * @param sender identity
     * @param contextInfo of message
     * @return plain text
     * @throws CipherException if decryption fails
     */
    byte[] decrypt(byte[] content, Identity sender, byte[] contextInfo) throws CipherException {
        return decrypt(content, sender.signatureKey(), contextInfo);
    }

    /**
     * Decrypts message
     * @param content to decrypt
     * @param sender id
     * @param signatureKey of sender
     * @return plain text
     * @throws CipherException if decryption fails
     */
    public byte[] decrypt(byte[] content, UUID sender, PublicKey signatureKey) throws CipherException {
        return decrypt(content, signatureKey, IO.writeUUIDToBytes(sender));
    }

    /**
     * Decrypts message
     * @param content to decrypt
     * @param signatureKey of sender
     * @param contextInfo of message
     * @return plain text
     * @throws CipherException if decryption fails
     */
    byte[] decrypt(byte[] content, PublicKey signatureKey, byte[] contextInfo) throws CipherException {
        try {
            HybridDecrypt hybridDecrypt = identity.dataKey.getPrimitive(HybridDecrypt.class);
            byte[] bytes = hybridDecrypt.decrypt(content, contextInfo);
            SignedMessage signedMessage = new SignedMessage(bytes);
            KeysetHandle handle = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(signatureKey.key));
            PublicKeyVerify verify = handle.getPrimitive(PublicKeyVerify.class);
            verify.verify(signedMessage.signature, signedMessage.contents);
            return signedMessage.contents;
        } catch (IOException | GeneralSecurityException e) {
            throw new UnknownCipherException("failed to decrypt data", e);
        }
    }

    /**
     * Encrypt for self storage
     * @param plain to encrypt
     * @throws CipherException if encryption fails
     * @return cipher text
     */
    public byte[] encrypt(byte[] plain) throws CipherException {
        try {
            HybridEncrypt hybridEncrypt = identity.dataKey.getPublicKeysetHandle().getPrimitive(HybridEncrypt.class);
            PublicKeySign sign = identity.signatureKey.getPrimitive(PublicKeySign.class);
            SignedMessage signedMessage = new SignedMessage(sign.sign(plain), plain);
            return hybridEncrypt.encrypt(signedMessage.serialize(), IO.writeUUIDToBytes(identityStore.identity().id()));
        } catch (GeneralSecurityException e) {
            throw new UnknownCipherException("failed to encrypt data", e);
        }
    }

    /**
     * Decrypt for self storage
     * @param bytes to encrypt
     * @throws CipherException if decryption fails
     * @return cipher text
     */
    public byte[] decrypt(byte[] bytes) throws CipherException {
        try {
            HybridDecrypt hybridDecrypt = identity.dataKey.getPrimitive(HybridDecrypt.class);
            byte[] plain = hybridDecrypt.decrypt(bytes, IO.writeUUIDToBytes(identityStore.identity().id()));
            SignedMessage signedMessage = new SignedMessage(plain);
            PublicKeyVerify verify = identity.signatureKey.getPublicKeysetHandle().getPrimitive(PublicKeyVerify.class);
            verify.verify(signedMessage.signature, signedMessage.contents);
            return signedMessage.contents;
        } catch (GeneralSecurityException e) {
            throw new UnknownCipherException("failed to decrypt data", e);
        }
    }
}
