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
 * Identity representing a client or server on the Collar protocol
 * For crypto purposes, use {@link com.collarmc.security.messages.Cipher<?>}
 * This class represents the identity.cif
 */
public final class CollarIdentity {
    private static final int VERSION = 2;

    public final UUID profile;
    public final ServerIdentity serverIdentity;
    public final KeysetHandle signatureKey;
    public final KeysetHandle dataKey;

    public CollarIdentity(UUID profile, ServerIdentity serverIdentity) throws UnavailableCipherException {
        this.profile = profile;
        this.serverIdentity = serverIdentity;
        try {
            this.signatureKey = KeysetHandle.generateNew(KeyTemplates.get("ECDSA_P256"));
            this.dataKey = KeysetHandle.generateNew(KeyTemplates.get("ECIES_P256_HKDF_HMAC_SHA256_AES128_CTR_HMAC_SHA256"));
        } catch (GeneralSecurityException e) {
            throw new UnavailableCipherException("could not generate new collar identity", e);
        }
    }

    public CollarIdentity(byte[] bytes) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            try (DataInputStream dataStream = new DataInputStream(inputStream)) {
                int version = dataStream.readInt();
                if (VERSION != version) {
                    throw new IllegalStateException("invalid version " + version);
                }
                profile = IO.readUUID(dataStream);
                signatureKey = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(IO.readBytes(dataStream)));
                dataKey = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(IO.readBytes(dataStream)));
                UUID serverId = IO.readUUID(dataStream);
                byte[] serverSigKey = IO.readBytes(dataStream);
                byte[] serverDataKey = IO.readBytes(dataStream);
                serverIdentity = new ServerIdentity(new PublicKey(serverDataKey), new PublicKey(serverSigKey), serverId);
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
                IO.writeUUID(dataStream, profile);
                IO.writeBytes(dataStream, serializeKey(signatureKey));
                IO.writeBytes(dataStream, serializeKey(dataKey));
                IO.writeUUID(dataStream, serverIdentity.serverId);
                IO.writeBytes(dataStream, serverIdentity.signatureKey.key);
                IO.writeBytes(dataStream, serverIdentity.publicKey.key);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("could not serialize identity.cif");
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
