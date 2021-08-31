package com.collarmc.security.messages;

import com.collarmc.api.identity.Identity;
import com.collarmc.security.PublicKey;
import com.google.common.io.ByteStreams;
import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;
import com.goterl.lazysodium.exceptions.SodiumException;
import com.goterl.lazysodium.interfaces.Box;
import com.goterl.lazysodium.interfaces.Sign;
import com.goterl.lazysodium.utils.KeyPair;
import com.goterl.lazysodium.utils.LibraryLoader;
import com.sun.jna.Platform;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import static java.util.Objects.isNull;

public final class SodiumCipher implements Cipher {

    private static final Logger LOGGER = LogManager.getLogger(SodiumCipher.class);
    private static boolean LOADED = false;
    private final KeyPair keyPair;
    private CollarSodium sodium;

    public SodiumCipher(CollarSodium sodium, KeyPair keyPair, boolean server) {
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
        byte[] sig = new byte[Sign.BYTES + plain.length];
        if (!sodium.getSodium().cryptoSign(sig, plain, plain.length, keyPair.getSecretKey().getAsBytes())) {
            throw new CipherException("Could not sign message.");
        }
        SignedMessage signedMessage = new SignedMessage(sig, plain);
        byte[] signedMessageBytes = signedMessage.serialize();
        byte[] cipherTextBytes = new byte[Box.SEALBYTES + signedMessageBytes.length];
        if (!sodium.getSodium().cryptoBoxSeal(cipherTextBytes, signedMessageBytes, signedMessageBytes.length, recipient)) {
            throw new CipherException("Could not encrypt message.");
        }
        return cipherTextBytes;
    }

    private byte[] decrypt(byte[] message, byte[] sender) throws CipherException {
        byte[] messageBytes = new byte[message.length - Box.SEALBYTES];
        if (!sodium.getSodium().cryptoBoxSealOpen(messageBytes, message, message.length, keyPair.getPublicKey().getAsBytes(), keyPair.getSecretKey().getAsBytes())) {
            throw new CipherException("Could not decrypt signed message.");
        }
        SignedMessage signedMessage = new SignedMessage(messageBytes);
        if (sodium.getSodium().cryptoSignOpen(signedMessage.signature, signedMessage.contents, signedMessage.contents.length, sender)) {
            throw new CipherException("Could not verify signed message.");
        }
        return signedMessage.contents;
    }
}
