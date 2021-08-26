package com.collarmc.security;

import com.collarmc.api.identity.ServerIdentity;
import com.collarmc.io.IO;
import com.collarmc.security.jce.JCECipher;
import com.collarmc.security.messages.CipherException;

import java.io.*;
import java.security.KeyPair;
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
    public final KeyPair keyPair;

    public CollarIdentity(byte[] bytes) throws CipherException, IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            try (DataInputStream dataStream = new DataInputStream(inputStream)) {
                int version = dataStream.readInt();
                if (VERSION != version) {
                    throw new IllegalStateException("invalid version " + version);
                }
                id = IO.readUUID(dataStream);
                keyPair = JCECipher.keyPair(IO.readBytes(dataStream), IO.readBytes(dataStream));
                if (dataStream.readBoolean()) {
                    UUID serverId = IO.readUUID(dataStream);
                    byte[] publicKey = IO.readBytes(dataStream);
                    this.serverIdentity = new ServerIdentity(serverId, new PublicKey(publicKey));
                } else {
                    this.serverIdentity = null;
                }
            }
        }
    }

    private CollarIdentity(UUID id, ServerIdentity serverIdentity) throws CipherException {
        this.id = id;
        this.serverIdentity = serverIdentity;
        this.keyPair = JCECipher.generateKeyPair();
    }

    private CollarIdentity(UUID id, byte[] publicKey, byte[] privateKey) throws CipherException {
        this.id = id;
        this.serverIdentity = null;
        this.keyPair = JCECipher.keyPair(publicKey, privateKey);
    }

    /**
     * @return the identity's public key
     */
    public PublicKey publicKey() {
        return new PublicKey(keyPair.getPublic().getEncoded());
    }

    public byte[] serialize() {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (DataOutputStream dataStream = new DataOutputStream(outputStream)) {
                dataStream.writeInt(VERSION);
                IO.writeUUID(dataStream, id);
                IO.writeBytes(dataStream, keyPair.getPublic().getEncoded());
                IO.writeBytes(dataStream, keyPair.getPrivate().getEncoded());
                dataStream.writeBoolean(serverIdentity != null);
                if (serverIdentity != null) {
                    IO.writeUUID(dataStream, serverIdentity.serverId);
                    IO.writeBytes(dataStream, serverIdentity.publicKey.key);
                }
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("could not serialize identity.cif");
        }
    }

    public static CollarIdentity createClientIdentity(UUID profile, ServerIdentity serverIdentity) throws CipherException {
        return new CollarIdentity(profile, serverIdentity);
    }

    public static CollarIdentity createServerIdentity() throws CipherException {
        return new CollarIdentity(UUID.randomUUID(), null);
    }

    public static CollarIdentity createServerIdentity(UUID profile, byte[] publicKey, byte[] privateKey) throws CipherException {
        return new CollarIdentity(profile, publicKey, privateKey);
    }
}
