package com.collarmc.security.sodium;

import com.collarmc.security.Key;
import com.collarmc.security.KeyPair;
import com.collarmc.security.messages.CipherException;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class Sodium {

    private final SodiumNative sodiumNative;

    public Sodium(SodiumNative sodiumNative) {
        this.sodiumNative = sodiumNative;
    }

    /**
     * Generates a public/private keypair.
     * @return public/private keypair
     * @throws CipherException
     */
    public KeyPair generateKeyPair() throws CipherException {
        byte[] publicKey = randomBytesBuf(32);
        byte[] secretKey = randomBytesBuf(32);
        if (!cryptoBoxKeypair(publicKey, secretKey)) {
            throw new CipherException("Unable to create a public and private key.");
        }
        return new KeyPair(Key.fromBytes(publicKey), Key.fromBytes(secretKey));
    }

    public byte[] randomBytesBuf(int size) {
        byte[] bs = new byte[size];
        sodiumNative.randombytes_buf(bs, size);
        return bs;
    }

    public boolean cryptoBoxKeypair(byte[] publicKey, byte[] secretKey) {
        return successful(sodiumNative.crypto_box_keypair(publicKey, secretKey));
    }

    public boolean cryptoSign(byte[] signedMessage, byte[] message, long messageLen, byte[] secretKey) {
        if (messageLen < 0 || messageLen > message.length) {
            throw new IllegalArgumentException("messageLen out of bounds: " + messageLen);
        }
        return successful(sodiumNative.crypto_sign(signedMessage, (new PointerByReference(Pointer.NULL)).getPointer(), message, messageLen, secretKey));
    }

    public boolean cryptoSignOpen(byte[] message, byte[] signedMessage, long signedMessageLen, byte[] publicKey) {
        if (signedMessageLen < 0 || signedMessageLen > signedMessage.length) {
            throw new IllegalArgumentException("signedMessageLen out of bounds: " + signedMessageLen);
        }
        return successful(sodiumNative.crypto_sign_open(message, (new PointerByReference(Pointer.NULL)).getPointer(), signedMessage, signedMessageLen, publicKey));
    }

    public boolean cryptoBoxSeal(byte[] cipher, byte[] message, long messageLen, byte[] publicKey) {
        if (messageLen < 0 || messageLen > message.length) {
            throw new IllegalArgumentException("messageLen out of bounds: " + messageLen);
        }
        return successful(sodiumNative.crypto_box_seal(cipher, message, messageLen, publicKey));
    }

    public boolean cryptoBoxSealOpen(byte[] m, byte[] cipher, long cipherLen, byte[] publicKey, byte[] secretKey) {
        if (cipherLen < 0 || cipherLen > cipher.length) {
            throw new IllegalArgumentException("cipherLen out of bounds: " + cipherLen);
        }
        return successful(sodiumNative.crypto_box_seal_open(m, cipher, cipherLen, publicKey, secretKey));
    }

    private static boolean successful(int res) {
        return (res == 0);
    }

    public static Sodium create() throws CipherException {
        String resourcePath = findResourcePath();
        File nativeLibPath;
        try {
            nativeLibPath = new File("collar", new File(resourcePath).getName());
            if (!nativeLibPath.getParentFile().exists() && !nativeLibPath.getParentFile().mkdirs()) {
                throw new IllegalStateException("could not create directories " + nativeLibPath.getParentFile());
            }
            if (nativeLibPath.exists() && !nativeLibPath.delete()) {
                throw new IllegalStateException("could not delete file " + nativeLibPath);
            }
            if (!nativeLibPath.createNewFile()) {
                throw new IllegalStateException("could not create file " + nativeLibPath);
            }
        } catch (IOException e) {
            throw new CipherException("could not create temp file", e);
        }
        try (InputStream from = Sodium.class.getClassLoader().getResourceAsStream(resourcePath);
             FileOutputStream to = new FileOutputStream(nativeLibPath)) {
            ByteStreams.copy(from, to);
        } catch (IOException e) {
            throw new CipherException("Problem loading sodium native from " + resourcePath);
        }
        SodiumNative sodiumNative = Native.loadLibrary(nativeLibPath.getAbsolutePath(), SodiumNative.class);
        return new Sodium(sodiumNative);
    }

    private static String findResourcePath() throws CipherException {
        boolean is64Bit = Native.POINTER_SIZE == 8;
        if (Platform.isWindows()) {
            if (is64Bit) {
                return getPath("windows64", "libsodium.dll");
            } else {
                return getPath("windows", "libsodium.dll");
            }
        }
        if (Platform.isMac()) {
            // check for Apple Silicon
            if(Platform.isARM()) {
                return getPath("mac/aarch64", "libsodium.dylib");
            } else {
                return getPath("mac/intel", "libsodium.dylib");
            }
        }
        if (Platform.isARM()) {
            return getPath("armv6", "libsodium.so");
        }
        if (Platform.isLinux()) {
            if (is64Bit) {
                return getPath("linux64", "libsodium.so");
            } else {
                return getPath("linux", "libsodium.so");
            }
        }
        throw new CipherException("could not load sodium for this platform");
    }

    private static String getPath(String folder, String name) {
        String separator = "/";
        return folder + separator + name;
    }

    public interface Sign {
        int ED25519_PUBLICKEYBYTES = 32,
                ED25519_BYTES = 64,
                ED25519_SECRETKEYBYTES = 64,
                ED25519_SEEDBYTES = 32,
                CURVE25519_PUBLICKEYBYTES = 32,
                CURVE25519_SECRETKEYBYTES = 32;

        long SIZE_MAX = Long.MAX_VALUE;

        long ED25519_MESSAGEBYTES_MAX = SIZE_MAX - ED25519_BYTES;

        int BYTES = ED25519_BYTES,
                PUBLICKEYBYTES = ED25519_PUBLICKEYBYTES,
                SECRETKEYBYTES = ED25519_SECRETKEYBYTES,
                SEEDBYTES = ED25519_SEEDBYTES;

        long MESSAGEBYTES_MAX = ED25519_MESSAGEBYTES_MAX;

        int crypto_sign_open(byte[] message, Pointer messageLen, byte[] signedMessage, long signedMessageLen, byte[] publicKey);
        int crypto_sign(byte[] signedMessage, Pointer sigLength, byte[] message, long messageLen, byte[] secretKey);
    }

    public interface Box {
        int CURVE25519XSALSA20POLY1305_PUBLICKEYBYTES = 32,
                CURVE25519XSALSA20POLY1305_SECRETKEYBYTES = 32,
                CURVE25519XSALSA20POLY1305_MACBYTES = 16,
                CURVE25519XSALSA20POLY1305_SEEDBYTES = 32,
                CURVE25519XSALSA20POLY1305_BEFORENMBYTES = 32,
                CURVE25519XSALSA20POLY1305_NONCEBYTES = 24;

        int PUBLICKEYBYTES = CURVE25519XSALSA20POLY1305_PUBLICKEYBYTES,
                SECRETKEYBYTES = CURVE25519XSALSA20POLY1305_SECRETKEYBYTES,
                MACBYTES = CURVE25519XSALSA20POLY1305_MACBYTES,
                SEEDBYTES = CURVE25519XSALSA20POLY1305_SEEDBYTES,
                BEFORENMBYTES = CURVE25519XSALSA20POLY1305_BEFORENMBYTES,
                NONCEBYTES = CURVE25519XSALSA20POLY1305_NONCEBYTES,
                SEALBYTES = PUBLICKEYBYTES + MACBYTES;

        int crypto_box_keypair(byte[] publicKey, byte[] secretKey);
        int crypto_box_seal(byte[] cipher, byte[] message, long messageLen, byte[] publicKey);
        int crypto_box_seal_open(byte[] m, byte[] cipher, long cipherLen, byte[] publicKey, byte[] secretKey);
        void randombytes_buf(byte[] buffer, int size);
    }

    public static final class KeyExchange {
        public static final int PUBLICKEYBYTES = 32;
        public static final int SECRETKEYBYTES = 32;
        public static final int SESSIONKEYBYTES = 32;
        public static final int SEEDBYTES = 32;
        public static final String PRIMITIVE = "x25519blake2b";
    }

    public interface SodiumNative extends Box,Sign,Library {}
}
