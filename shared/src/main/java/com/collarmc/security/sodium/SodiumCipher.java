package com.collarmc.security.sodium;

import com.collarmc.api.identity.Identity;
import com.collarmc.security.KeyPair;
import com.collarmc.security.PublicKey;
import com.collarmc.security.messages.Cipher;
import com.collarmc.security.messages.CipherException;
import com.collarmc.security.messages.SignedMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SodiumCipher implements Cipher {

    private static final Logger LOGGER = LogManager.getLogger(SodiumCipher.class);
    private final KeyPair keyPair;
    private final Sodium sodium;

    public SodiumCipher(Sodium sodium, KeyPair keyPair) {
        this.sodium = sodium;
        this.keyPair = keyPair;
    }

    @Override
    public byte[] decrypt(byte[] content, PublicKey publicKey) throws CipherException {
        return decrypt(content, publicKey.key);
    }

    @Override
    public byte[] decrypt(byte[] content, Identity sender) throws CipherException {
        return decrypt(content, sender.publicKey().key);
    }

    @Override
    public byte[] decrypt(byte[] bytes) throws CipherException {
        return decrypt(bytes, keyPair.getPublicKey().getAsBytes());
    }

    @Override
    public byte[] encrypt(byte[] plain, Identity recipient) throws CipherException {
        return encrypt(plain, recipient.publicKey().key);
    }

    @Override
    public byte[] encrypt(byte[] plain, PublicKey recipient) throws CipherException {
        return encrypt(plain, recipient.key);
    }

    @Override
    public byte[] encrypt(byte[] plain) throws CipherException {
        return encrypt(plain, keyPair.getPublicKey().getAsBytes());
    }

    private byte[] encrypt(byte[] plain, byte[] recipient) throws CipherException {
        byte[] sig = new byte[Sodium.Sign.BYTES + plain.length];
        if (!sodium.cryptoSign(sig, plain, plain.length, keyPair.getSecretKey().getAsBytes())) {
            throw new CipherException("Could not sign message.");
        }
        SignedMessage signedMessage = new SignedMessage(sig, plain);
        byte[] signedMessageBytes = signedMessage.serialize();
        byte[] cipherTextBytes = new byte[Sodium.Box.SEALBYTES + signedMessageBytes.length];
        if (!sodium.cryptoBoxSeal(cipherTextBytes, signedMessageBytes, signedMessageBytes.length, recipient)) {
            throw new CipherException("Could not encrypt message.");
        }
        return cipherTextBytes;
    }

    private byte[] decrypt(byte[] message, byte[] sender) throws CipherException {
        byte[] messageBytes = new byte[message.length - Sodium.Box.SEALBYTES];
        if (!sodium.cryptoBoxSealOpen(messageBytes, message, message.length, keyPair.getPublicKey().getAsBytes(), keyPair.getSecretKey().getAsBytes())) {
            throw new CipherException("Could not decrypt signed message.");
        }
        SignedMessage signedMessage = new SignedMessage(messageBytes);
        if (sodium.cryptoSignOpen(signedMessage.signature, signedMessage.contents, signedMessage.contents.length, sender)) {
            throw new CipherException("Could not verify signed message.");
        }
        return signedMessage.contents;
    }
}
