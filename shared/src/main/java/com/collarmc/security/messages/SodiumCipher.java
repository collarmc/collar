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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class SodiumCipher implements Cipher {

    private static final LazySodiumJava SODIUM;

    static {
        String path = LibraryLoader.getSodiumPathInResources();
        File jar;
        try {
            jar = File.createTempFile("sodium", ".lib");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (InputStream is = SodiumCipher.class.getResourceAsStream("/" + path);
            FileOutputStream os = new FileOutputStream(jar)) {
            ByteStreams.copy(is, os);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SODIUM = new LazySodiumJava(new SodiumJava(jar.getAbsolutePath()));
    }

    private final KeyPair keyPair;

    public SodiumCipher(KeyPair keyPair) {
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
    public byte[] encrypt(byte[] plain) throws CipherException {
        return encrypt(plain, keyPair.getPublicKey().getAsBytes());
    }

    private byte[] encrypt(byte[] plain, byte[] recipient) throws CipherException {
        byte[] sig = new byte[Sign.BYTES + plain.length];
        if (!SODIUM.cryptoSign(sig, plain, plain.length, keyPair.getSecretKey().getAsBytes())) {
            throw new CipherException("Could not sign message.");
        }
        SignedMessage signedMessage = new SignedMessage(sig, plain);
        byte[] signedMessageBytes = signedMessage.serialize();
        byte[] cipherTextBytes = new byte[Box.SEALBYTES + signedMessageBytes.length];

        if (!SODIUM.cryptoBoxSeal(cipherTextBytes, signedMessageBytes, signedMessageBytes.length, recipient)) {
            throw new CipherException("Could not encrypt message.");
        }
        return cipherTextBytes;
    }

    private byte[] decrypt(byte[] message, byte[] sender) throws CipherException {
        byte[] messageBytes = new byte[message.length - Box.SEALBYTES];
        if (!SODIUM.cryptoBoxSealOpen(messageBytes, message, message.length, keyPair.getPublicKey().getAsBytes(), keyPair.getSecretKey().getAsBytes())) {
            throw new CipherException("Could not decrypt signedMessage.");
        }
        SignedMessage signedMessage = new SignedMessage(messageBytes);
        if (SODIUM.cryptoSignOpen(signedMessage.signature, signedMessage.contents, signedMessage.contents.length, sender)) {
            throw new CipherException("Could not verify signedMessage.");
        }
        return signedMessage.contents;
    }

    public static KeyPair generateKeyPair() throws CipherException {
        try {
            return SODIUM.cryptoBoxKeypair();
        } catch (SodiumException e) {
            throw new CipherException("could not generate key pair", e);
        }
    }
}
