package com.collarmc.security.messages;

import com.collarmc.api.identity.Identity;
import com.collarmc.security.CollarIdentity;
import com.collarmc.security.PublicKey;
import com.collarmc.security.jce.JCECipher;

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
     * Decrypt message from sender
     * @param content of message
     * @param publicKey of message
     * @return plain text
     * @throws CipherException if decryption fails
     */
    public byte[] decrypt(byte[] content, PublicKey publicKey) throws CipherException {
        byte[] plainText = JCECipher.decrypt(content, identity.keyPair.getPrivate());
        SignedMessage message = new SignedMessage(plainText);
        JCECipher.verify(message.contents, message.signature, JCECipher.publicKey(publicKey.key));
        return message.contents;
    }

    /**
     * Decrypt message from sender
     * @param content of message
     * @param sender of message
     * @return plain text
     * @throws CipherException if decryption fails
     */
    public byte[] decrypt(byte[] content, Identity sender) throws CipherException {
        byte[] plainText = JCECipher.decrypt(content, identity.keyPair.getPrivate());
        SignedMessage message = new SignedMessage(plainText);
        JCECipher.verify(message.contents, message.signature, JCECipher.publicKey(sender.publicKey().key));
        return message.contents;
    }

    /**
     * Decrypt for self storage
     * @param bytes to encrypt
     * @throws CipherException if decryption fails
     * @return cipher text
     */
    public byte[] decrypt(byte[] bytes) throws CipherException {
        byte[] plain = JCECipher.decrypt(bytes, identity.keyPair.getPrivate());
        SignedMessage signedMessage = new SignedMessage(plain);
        JCECipher.verify(signedMessage.contents, signedMessage.signature, identity.keyPair.getPublic());
        return signedMessage.contents;
    }

    /**
     * Encrypts data for an intended recipient
     * @param plain to encrypt
     * @param recipient receiving the message
     * @return cipher text
     * @throws CipherException if encryption fails
     */
    public byte[] encrypt(byte[] plain, Identity recipient) throws CipherException {
        byte[] signature = JCECipher.sign(plain, identity.keyPair.getPrivate());
        SignedMessage signedMessage = new SignedMessage(signature, plain);
        return JCECipher.encrypt(signedMessage.serialize(), JCECipher.publicKey(recipient.publicKey().key));
    }

    /**
     * Encrypt for self storage
     * @param plain to encrypt
     * @throws CipherException if encryption fails
     * @return cipher text
     */
    public byte[] encrypt(byte[] plain) throws CipherException {
        SignedMessage signedMessage = new SignedMessage(JCECipher.sign(plain, identity.keyPair.getPrivate()), plain);
        return JCECipher.encrypt(signedMessage.contents, identity.keyPair.getPublic());
    }
}
