package com.collarmc.security;

import com.collarmc.api.identity.ServerIdentity;
import com.collarmc.io.IO;
import com.collarmc.security.messages.CipherException.UnavailableCipherException;
import com.google.crypto.tink.*;
import com.google.crypto.tink.config.TinkConfig;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.UUID;

/**
 * Represents the private keys and identity of a client or server using the Collar protocol
 * For crypto purposes, use {@link com.collarmc.security.messages.Cipher<?>}
 * This class represents the identity.cif
 */
public final class CollarIdentity {
    private static final int VERSION = 2;

    public final UUID id;
    public final ServerIdentity serverIdentity;
    public final KeysetHandle signatureKey;
    public final KeysetHandle dataKey;

    public CollarIdentity(byte[] bytes) throws UnavailableCipherException, IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            try (DataInputStream dataStream = new DataInputStream(inputStream)) {
                int version = dataStream.readInt();
                if (VERSION != version) {
                    throw new IllegalStateException("invalid version " + version);
                }
                id = IO.readUUID(dataStream);
                signatureKey = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(IO.readBytes(dataStream)));
                dataKey = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(IO.readBytes(dataStream)));
                if (dataStream.readBoolean()) {
                    UUID serverId = IO.readUUID(dataStream);
                    byte[] serverSigKey = IO.readBytes(dataStream);
                    byte[] serverDataKey = IO.readBytes(dataStream);
                    this.serverIdentity = new ServerIdentity(new PublicKey(serverDataKey), new PublicKey(serverSigKey), serverId);
                } else {
                    this.serverIdentity = null;
                }
            }
        } catch (GeneralSecurityException e) {
            throw new UnavailableCipherException("problem reading keys from identity.cif", e);
        }
    }

    private CollarIdentity(UUID id, ServerIdentity serverIdentity) throws UnavailableCipherException {
        this.id = id;
        this.serverIdentity = serverIdentity;
        try {
            this.signatureKey = KeysetHandle.generateNew(KeyTemplates.get("ECDSA_P256"));
            this.dataKey = KeysetHandle.generateNew(KeyTemplates.get("ECIES_P256_HKDF_HMAC_SHA256_AES128_CTR_HMAC_SHA256"));
        } catch (GeneralSecurityException e) {
            throw new UnavailableCipherException("could not generate new collar identity", e);
        }
    }

    private CollarIdentity(UUID id, byte[] dataKey, byte[] signatureKey) throws UnavailableCipherException, IOException {
        this.id = id;
        this.serverIdentity = null;
        try {
            this.signatureKey = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(signatureKey));
            this.dataKey = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(dataKey));
        } catch (GeneralSecurityException e) {
            throw new UnavailableCipherException("could not generate new collar identity", e);
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
                IO.writeUUID(dataStream, id);
                IO.writeBytes(dataStream, serializeKey(signatureKey));
                IO.writeBytes(dataStream, serializeKey(dataKey));
                dataStream.writeBoolean(serverIdentity != null);
                if (serverIdentity != null) {
                    IO.writeUUID(dataStream, serverIdentity.serverId);
                    IO.writeBytes(dataStream, serverIdentity.signatureKey.key);
                    IO.writeBytes(dataStream, serverIdentity.publicKey.key);
                }
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("could not serialize identity.cif");
        }
    }

    public static CollarIdentity createClientIdentity(UUID profile, ServerIdentity serverIdentity) throws UnavailableCipherException {
        return new CollarIdentity(profile, serverIdentity);
    }

    public static CollarIdentity createServerIdentity() throws UnavailableCipherException {
        return new CollarIdentity(UUID.randomUUID(), null);
    }

    public static CollarIdentity createServerIdentity(UUID profile, byte[] dataKey, byte[] signatureKey) throws UnavailableCipherException, IOException {
        return new CollarIdentity(profile, dataKey, signatureKey);
    }

    public static byte[] serializeKey(KeysetHandle handle) {
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
