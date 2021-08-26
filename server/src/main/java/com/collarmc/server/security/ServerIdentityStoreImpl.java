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

public class ServerIdentityStoreImpl implements ServerIdentityStore {

    private final CollarIdentity collarIdentity;

    public ServerIdentityStoreImpl(MongoDatabase database) throws CipherException, IOException {
        MongoCollection<Document> serverIdentityCollection = database.getCollection("server_identity");
        MongoCursor<Document> iterator = serverIdentityCollection.find().iterator();
        if (iterator.hasNext()) {
            Document document = iterator.next();
            Binary publicKey = document.get("publicKey", Binary.class);
            Binary privateKey = document.get("privateKey", Binary.class);
            UUID serverId = document.get("serverId", UUID.class);
            collarIdentity = CollarIdentity.createServerIdentity(serverId, publicKey.getData(), privateKey.getData());
        } else {
            collarIdentity = CollarIdentity.createServerIdentity();
            Document document = new Document(Map.of(
                    "serverId", collarIdentity.id,
                    "publicKey", new Binary(collarIdentity.keyPair.getPublic().getEncoded()),
                    "privateKey", new Binary(collarIdentity.keyPair.getPrivate().getEncoded())
            ));
            serverIdentityCollection.insertOne(document);
        }
    }

    @Override
    public ServerIdentity identity() {
        return new ServerIdentity(collarIdentity.id, collarIdentity.publicKey());
    }

    @Override
    public Cipher cipher() {
        return new Cipher(collarIdentity);
    }
}
