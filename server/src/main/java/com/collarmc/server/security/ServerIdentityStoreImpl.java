package com.collarmc.server.security;

import com.collarmc.api.identity.ServerIdentity;
import com.collarmc.security.CollarIdentity;
import com.collarmc.security.messages.Cipher;
import com.collarmc.security.messages.CipherException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.Binary;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static com.collarmc.security.CollarIdentity.serializeKey;

public class ServerIdentityStoreImpl implements ServerIdentityStore {

    private final CollarIdentity collarIdentity;

    public ServerIdentityStoreImpl(MongoDatabase database) throws CipherException, IOException {
        MongoCollection<Document> serverIdentityCollection = database.getCollection("server_identity");
        MongoCursor<Document> iterator = serverIdentityCollection.find().iterator();
        if (iterator.hasNext()) {
            Document document = iterator.next();
            Binary dataKey = document.get("dataKey", Binary.class);
            Binary signatureKey = document.get("signatureKey", Binary.class);
            UUID serverId = document.get("serverId", UUID.class);
            collarIdentity = CollarIdentity.createServerIdentity(serverId, dataKey.getData(), signatureKey.getData());
        } else {
            collarIdentity = CollarIdentity.createServerIdentity();
            Document document = new Document(Map.of(
                    "serverId", collarIdentity.id,
                    "signatureKey", new Binary(serializeKey(collarIdentity.signatureKey)),
                    "dataKey", new Binary(serializeKey(collarIdentity.dataKey))
            ));
            serverIdentityCollection.insertOne(document);
        }
    }

    @Override
    public ServerIdentity identity() {
        return new ServerIdentity(collarIdentity.publicKey(), collarIdentity.signatureKey(), collarIdentity.id);
    }

    @Override
    public Cipher<ServerIdentity> cipher() {
        return new Cipher<>(this.identity(), this, collarIdentity);
    }
}
