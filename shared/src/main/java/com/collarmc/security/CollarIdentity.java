package com.collarmc.security;

import com.collarmc.api.identity.ServerIdentity;
import com.collarmc.io.IO;
import com.collarmc.security.messages.*;
import com.goterl.lazysodium.utils.Key;
import com.goterl.lazysodium.utils.KeyPair;

import java.io.*;
import java.util.UUID;

/**
 * Represents the private keys and identity of a client or server using the Collar protocol
 * For crypto purposes, use {@link IdentityStore#cipher()}
 * This class represents the identity.cif
 */
public final class CollarIdentity {
    private static final int VERSION = 2;

    public final UUID id;
    public final ServerIdentity serverIdentity;
    public final KeyPair keyPair;

    private CollarIdentity(byte[] bytes) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            try (DataInputStream dataStream = new DataInputStream(inputStream)) {
                int version = dataStream.readInt();
                if (VERSION != version) {
                    throw new IllegalStateException("invalid version " + version);
                }
                id = IO.readUUID(dataStream);
                Key publicKey = Key.fromBytes(IO.readBytes(dataStream));
                Key privateKey = Key.fromBytes(IO.readBytes(dataStream));
                keyPair = new KeyPair(publicKey, privateKey);
                if (dataStream.readBoolean()) {
                    UUID serverId = IO.readUUID(dataStream);
                    byte[] serverKey = IO.readBytes(dataStream);
                    this.serverIdentity = new ServerIdentity(serverId, new PublicKey(serverKey));
                } else {
                    this.serverIdentity = null;
                }
            }
        }
    }

    private CollarIdentity(UUID id, ServerIdentity serverIdentity, KeyPair keyPair) throws CipherException {
        this.id = id;
        this.serverIdentity = serverIdentity;
        this.keyPair = keyPair;
    }

    private CollarIdentity(UUID id, byte[] publicKey, byte[] privateKey) {
        this.id = id;
        this.serverIdentity = null;
        this.keyPair = new KeyPair(Key.fromBytes(publicKey), Key.fromBytes(privateKey));
    }

    /**
     * @return the identity's public key
     */
    public PublicKey publicKey() {
        return new PublicKey(keyPair.getPublicKey().getAsBytes());
    }

    public byte[] serialize() {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (DataOutputStream dataStream = new DataOutputStream(outputStream)) {
                dataStream.writeInt(VERSION);
                IO.writeUUID(dataStream, id);
                IO.writeBytes(dataStream, keyPair.getPublicKey().getAsBytes());
                IO.writeBytes(dataStream, keyPair.getSecretKey().getAsBytes());
                dataStream.writeBoolean(serverIdentity != null);
                if (serverIdentity != null) {
                    IO.writeUUID(dataStream, serverIdentity.id());
                    IO.writeBytes(dataStream, serverIdentity.publicKey().key);
                }
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("could not serialize identity.cif");
        }
    }

    public static CollarIdentity createClientIdentity(UUID profile, ServerIdentity serverIdentity, CollarSodium sodium) throws CipherException {
        return new CollarIdentity(profile, serverIdentity, sodium.generateKeyPair());
    }

    public static CollarIdentity createServerIdentity(CollarSodium sodium) throws CipherException {
        return new CollarIdentity(UUID.randomUUID(), null, sodium.generateKeyPair());
    }

    public static CollarIdentity serverIdentityFrom(UUID profile, byte[] publicKey, byte[] privateKey) {
        return new CollarIdentity(profile, publicKey, privateKey);
    }

    public static CollarIdentity from(byte[] bytes) throws CipherException, IOException {
        return new CollarIdentity(bytes);
    }
}
