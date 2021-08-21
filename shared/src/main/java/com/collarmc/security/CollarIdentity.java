package com.collarmc.security;

import com.collarmc.io.AtomicFile;
import com.collarmc.io.IO;
import com.collarmc.security.messages.CipherException;
import com.collarmc.security.messages.CipherException.UnknownCipherException;
import com.google.crypto.tink.*;
import com.google.crypto.tink.config.TinkConfig;
import com.google.crypto.tink.signature.SignatureKeyTemplates;

import java.io.*;
import java.security.GeneralSecurityException;

/**
 * Identity representing a client or server on the Collar protocol
 * For crypto purposes, use {@link com.collarmc.security.messages.Cipher<?>}
 */
public final class CollarIdentity {
    private static final int VERSION = 2;

    public final KeysetHandle signatureKey;
    public final KeysetHandle dataKey;

    public CollarIdentity() {
        try {
            this.signatureKey = KeysetHandle.generateNew(SignatureKeyTemplates.ED25519);
            this.dataKey = KeysetHandle.generateNew(KeyTemplates.get("ECIES_P256_HKDF_HMAC_SHA256_AES128_CTR_HMAC_SHA256"));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("could not generate new collar identity", e);
        }
    }

    public CollarIdentity(byte[] bytes) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            try (DataInputStream dataStream = new DataInputStream(inputStream)) {
                int version = dataStream.readInt();
                if (VERSION != version) {
                    throw new IllegalStateException("invalid version " + version);
                }
                signatureKey = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(IO.readBytes(dataStream)));
                dataKey = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(IO.readBytes(dataStream)));
            }
        } catch (IOException|GeneralSecurityException e) {
            throw new IllegalStateException("could not read identity.cif", e);
        }
    }

    /**
     * @return the identity's public key
     */
    public PublicKey publicKey() {
        try {
            return new PublicKey(serializeKey(dataKey.getPublicKeysetHandle()));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("could not serialize public key");
        }
    }

    /**
     * @return the identity's signature key
     */
    public PublicKey signatureKey() {
        try {
            return new PublicKey(serializeKey(signatureKey.getPublicKeysetHandle()));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("could not serialize public key");
        }
    }

    public byte[] serialize() {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (DataOutputStream dataStream = new DataOutputStream(outputStream)) {
                dataStream.writeInt(VERSION);
                IO.writeBytes(dataStream, serializeKey(signatureKey));
                IO.writeBytes(dataStream, serializeKey(dataKey));
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("could not serialize identity.cif");
        }
    }

    /**
     * Creates or generates a new private identity for a client
     * @param profileDirectory in collar client home
     * @return private identity
     */
    public static CollarIdentity getOrCreate(File profileDirectory) {
        File file = new File(profileDirectory, "identity.cif");
        if (file.exists()) {
            byte[] bytes = IO.readBytesFromFile(file);
            return new CollarIdentity(bytes);
        } else {
            CollarIdentity collarIdentity = new CollarIdentity();
            try {
                AtomicFile.write(file, theFile -> IO.writeBytesToFile(theFile, collarIdentity.serialize()));
            } catch (IOException e) {
                throw new IllegalStateException("could not write private identity", e);
            }
            return collarIdentity;
        }
    }

    public byte[] encrypt(byte[] bytes) throws CipherException {
        try {
            HybridEncrypt hybridEncrypt = dataKey.getPublicKeysetHandle().getPrimitive(HybridEncrypt.class);
            return hybridEncrypt.encrypt(bytes, null);
        } catch (GeneralSecurityException e) {
            throw new UnknownCipherException("could not encrypt data", e);
        }
    }

    public byte[] decrypt(byte[] bytes) throws CipherException {
        try {
            HybridDecrypt hybridDecrypt = dataKey.getPrimitive(HybridDecrypt.class);
            return hybridDecrypt.decrypt(bytes, null);
        } catch (GeneralSecurityException e) {
            throw new UnknownCipherException("could not encrypt data", e);
        }
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
            TinkConfig.init();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
