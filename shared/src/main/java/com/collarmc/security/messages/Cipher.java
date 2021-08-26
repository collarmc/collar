package com.collarmc.security.messages;

import com.collarmc.api.identity.Identity;
import com.collarmc.security.CollarIdentity;
import com.collarmc.security.PublicKey;
import com.collarmc.security.jce.JCECipher;

/**
 * Message Cipher service for encrypting and decrypting protocol messages
 * All messages processed with this service are encrypted and signed
 */
public final class Cipher {

    private final CollarIdentity identity;

    public Cipher(CollarIdentity identity) {
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
        return decrypt(content, JCECipher.publicKey(publicKey.key));
    }

    /**
     * Decrypt message from sender
     * @param content of message
     * @param sender of message
     * @return plain text
     * @throws CipherException if decryption fails
     */
    public byte[] decrypt(byte[] content, Identity sender) throws CipherException {
        return decrypt(content, JCECipher.publicKey(sender.publicKey().key));
    }

    /**
     * Decrypt for self storage
     * @param bytes to encrypt
     * @throws CipherException if decryption fails
     * @return cipher text
     */
    public byte[] decrypt(byte[] bytes) throws CipherException {
        return decrypt(bytes, identity.keyPair.getPublic());
    }

    /**
     * Encrypts data for an intended recipient
     * @param plain to encrypt
     * @param recipient receiving the message
     * @return cipher text
     * @throws CipherException if encryption fails
     */
    public byte[] encrypt(byte[] plain, Identity recipient) throws CipherException {
        return encrypt(plain, JCECipher.publicKey(recipient.publicKey().key));
    }

    /**
     * Encrypt for self storage
     * @param plain to encrypt
     * @throws CipherException if encryption fails
     * @return cipher text
     */
    public byte[] encrypt(byte[] plain) throws CipherException {
        return encrypt(plain, identity.keyPair.getPublic());
    }

    private byte[] encrypt(byte[] plain, java.security.PublicKey recipient) throws CipherException {
        SignedMessage signedMessage = new SignedMessage(JCECipher.sign(plain, identity.keyPair.getPrivate()), plain);
        return JCECipher.encrypt(signedMessage.serialize(), recipient);
    }

    private byte[] decrypt(byte[] content, java.security.PublicKey sender) throws CipherException {
        byte[] plainText = JCECipher.decrypt(content, identity.keyPair.getPrivate());
        SignedMessage message = new SignedMessage(plainText);
        JCECipher.verify(message.contents, message.signature, sender);
        return message.contents;
    }
}
