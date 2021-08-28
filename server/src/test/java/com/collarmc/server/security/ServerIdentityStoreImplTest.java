package com.collarmc.server.security;

import com.collarmc.security.TokenGenerator;
import com.collarmc.server.junit.MongoDatabaseTestRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

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
        ServerIdentityStoreImpl identityStore = new ServerIdentityStoreImpl(dbRule.db);
        byte[] token = TokenGenerator.byteToken(256);
        byte[] cipherText = identityStore.cipher().encrypt(token);
        byte[] decrypted = identityStore.cipher().decrypt(cipherText);
        Assert.assertArrayEquals(token, decrypted);
    }
}
