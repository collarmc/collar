package com.collarmc.server.security;

import com.collarmc.security.Identity;
import com.collarmc.security.PublicKey;
import com.collarmc.security.ServerIdentity;
import com.collarmc.security.discrete.Cipher;
import com.google.crypto.tink.BinaryKeysetWriter;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.hybrid.HybridConfig;
import com.google.crypto.tink.signature.SignatureConfig;
import com.google.crypto.tink.signature.SignatureKeyTemplates;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.UUID;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class ServerIdentityStoreImpl implements ServerIdentityStore {

    private final MongoCollection<Document> identities;
    private final MongoCollection<Document> serverIdentity;
    private final UUID serverId = UUID.randomUUID();
    private final KeysetHandle identityPrivateKey;
    private final KeysetHandle identityPublicKey;

    public ServerIdentityStoreImpl(MongoDatabase database) throws GeneralSecurityException {
        this.identities = database.getCollection("discrete_identities");
        this.identities.createIndex(new Document(Map.of("profile", 1)));
        this.serverIdentity = database.getCollection("discrete_server_identity");
        this.identityPrivateKey = KeysetHandle.generateNew(SignatureKeyTemplates.ECDSA_P256);
        this.identityPublicKey = identityPrivateKey.getPublicKeysetHandle();
    }

    @Override
    public ServerIdentity identity() {
        try {
            return new ServerIdentity(new PublicKey(serializeKey(identityPrivateKey.getPublicKeysetHandle())), serverId);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void trustIdentity(Identity identity) {
        Map<String, Object> state = Map.of("profile", identity.id(), "publicKey", identity.publicKey().key);
        identities.insertOne(new Document(state));
    }

    @Override
    public boolean isTrustedIdentity(Identity identity) {
        MongoCursor<Document> cursor = identities.find(
            and(
                eq("profile", identity.id()),
                eq("publicKey", identity.publicKey().key)
            )
        ).cursor();
        return cursor.hasNext();
    }

    @Override
    public Cipher<ServerIdentity> cipher() {
        return new Cipher<>(this.identity(), this, null, identityPrivateKey);
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
            HybridConfig.register();
            SignatureConfig.register();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
