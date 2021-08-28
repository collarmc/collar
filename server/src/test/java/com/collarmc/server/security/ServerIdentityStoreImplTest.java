package com.collarmc.server.security;

import com.collarmc.security.CollarIdentity;
import com.collarmc.security.TokenGenerator;
import com.collarmc.security.messages.SodiumCipher;
import com.collarmc.server.junit.MongoDatabaseTestRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.UUID;

public class ServerIdentityStoreImplTest {
    @Rule
    public MongoDatabaseTestRule dbRule = new MongoDatabaseTestRule();

    @Test
    public void roundTrip() throws Exception {
        ServerIdentityStoreImpl identityStore = new ServerIdentityStoreImpl(dbRule.db);
        ServerIdentityStoreImpl identityStore2 = new ServerIdentityStoreImpl(dbRule.db);
        Assert.assertEquals(identityStore.identity(), identityStore2.identity());
        Assert.assertEquals(identityStore.identity().id(), identityStore2.identity().id());
        Assert.assertArrayEquals(identityStore.identity().publicKey().key, identityStore2.identity().publicKey().key);
    }

    @Test
    public void cipher() throws Exception {
        // Load server once to make sure we create the identity
        new ServerIdentityStoreImpl(dbRule.db);
        // Load server identity from the database again
        ServerIdentityStoreImpl server = new ServerIdentityStoreImpl(dbRule.db);
        // encrypt message
        byte[] token = TokenGenerator.byteToken(256);
        byte[] cipherText = server.cipher().encrypt(token);
        // Decrypt message
        byte[] decrypted = server.cipher().decrypt(cipherText);
        Assert.assertArrayEquals(token, decrypted);
    }

    @Test
    public void serverSendsMessageToBob() throws Exception {
        // Load server once to make sure we create the identity
        new ServerIdentityStoreImpl(dbRule.db);
        // Load server identity from the database again
        ServerIdentityStoreImpl server = new ServerIdentityStoreImpl(dbRule.db);
        // Create bob
        CollarIdentity bob = CollarIdentity.createClientIdentity(UUID.randomUUID(), server.identity());
        // Encrypt message for bob
        byte[] token = TokenGenerator.byteToken(256);
        byte[] cipherText = server.cipher().encrypt(token, bob.publicKey());
        // Bob gets the message
        byte[] decrypted = new SodiumCipher(bob.keyPair, false).decrypt(cipherText, server.identity().publicKey());
        Assert.assertArrayEquals(token, decrypted);
    }

    static {
        SodiumCipher.loadLibrary(false);
    }
}
