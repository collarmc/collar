package com.collarmc.server.security;

import com.collarmc.api.identity.ServerIdentity;
import com.collarmc.security.CollarIdentity;
import com.collarmc.security.messages.Cipher;
import com.collarmc.security.messages.CipherException;
import com.collarmc.security.messages.SodiumCipher;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.types.Binary;

import java.util.Map;
import java.util.UUID;

public class ServerIdentityStoreImpl implements ServerIdentityStore {

    private static final Logger LOGGER = LogManager.getLogger(ServerIdentityStoreImpl.class);

    private final CollarIdentity collarIdentity;

    public ServerIdentityStoreImpl(MongoDatabase database) throws CipherException {
        MongoCollection<Document> serverIdentityCollection = database.getCollection("server_identity");
        MongoCursor<Document> iterator = serverIdentityCollection.find().iterator();
        if (iterator.hasNext()) {
            Document document = iterator.next();
            Binary publicKey = document.get("publicKey", Binary.class);
            Binary privateKey = document.get("privateKey", Binary.class);
            UUID serverId = document.get("serverId", UUID.class);
            collarIdentity = CollarIdentity.from(serverId, publicKey.getData(), privateKey.getData());
            LOGGER.info("Found server identity " + collarIdentity.id);
        } else {
            collarIdentity = CollarIdentity.createServerIdentity();
            Document document = new Document(Map.of(
                    "serverId", collarIdentity.id,
                    "publicKey", new Binary(collarIdentity.keyPair.getPublicKey().getAsBytes()),
                    "privateKey", new Binary(collarIdentity.keyPair.getSecretKey().getAsBytes())
            ));
            serverIdentityCollection.insertOne(document);
            LOGGER.info("Created new server identity " + collarIdentity.id);
        }
    }

    @Override
    public ServerIdentity identity() {
        return new ServerIdentity(collarIdentity.id, collarIdentity.publicKey());
    }

    @Override
    public Cipher cipher() {
        return new SodiumCipher(collarIdentity.keyPair);
    }
}
