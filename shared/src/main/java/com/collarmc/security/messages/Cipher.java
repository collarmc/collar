package com.collarmc.security.messages;

import com.collarmc.api.identity.Identity;
import com.collarmc.api.security.PublicKey;

public interface Cipher {
    /**
     * Decrypt message from sender
     *
     * @param content   of message
     * @param publicKey of message
     * @return plain text
     * @throws CipherException if decryption fails
     */
    byte[] decrypt(byte[] content, PublicKey publicKey) throws CipherException;

    /**
     * Decrypt message from sender
     *
     * @param content of message
     * @param sender  of message
     * @return plain text
     * @throws CipherException if decryption fails
     */
    byte[] decrypt(byte[] content, Identity sender) throws CipherException;

    /**
     * Decrypt for self storage
     *
     * @param bytes to encrypt
     * @return cipher text
     * @throws CipherException if decryption fails
     */
    byte[] decrypt(byte[] bytes) throws CipherException;

    /**
     * Encrypts data for an intended recipient
     *
     * @param plain     to encrypt
     * @param recipient receiving the message
     * @return cipher text
     * @throws CipherException if encryption fails
     */
    byte[] encrypt(byte[] plain, Identity recipient) throws CipherException;

    /**
     * Encrypts data for an intended recipient
     *
     * @param plain     to encrypt
     * @param recipient receiving the message
     * @return cipher text
     * @throws CipherException if encryption fails
     */
    byte[] encrypt(byte[] plain, PublicKey recipient) throws CipherException;

    /**
     * Encrypt for self storage
     *
     * @param plain to encrypt
     * @return cipher text
     * @throws CipherException if encryption fails
     */
    byte[] encrypt(byte[] plain) throws CipherException;
}
