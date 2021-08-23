package com.collarmc.server.security;

import com.collarmc.api.identity.Identity;
import com.collarmc.api.identity.ServerIdentity;
import com.collarmc.security.CollarIdentity;
import com.collarmc.security.messages.Cipher;
import com.collarmc.security.messages.CipherException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.UUID;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class ServerIdentityStoreImpl implements ServerIdentityStore {

    private final MongoCollection<Document> identities;
    private final MongoCollection<Document> serverIdentity;
    private final UUID serverId = UUID.randomUUID();
    private final CollarIdentity collarIdentity;

    public ServerIdentityStoreImpl(MongoDatabase database) throws CipherException {
        this.identities = database.getCollection("discrete_identities");
        this.identities.createIndex(new Document(Map.of("profile", 1)));
        this.serverIdentity = database.getCollection("discrete_server_identity");
        this.collarIdentity = new CollarIdentity();
    }

    @Override
    public ServerIdentity identity() {
        return new ServerIdentity(collarIdentity.publicKey(), collarIdentity.signatureKey(), serverId);
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
        return new Cipher<>(this.identity(), this, collarIdentity);
    }
}
