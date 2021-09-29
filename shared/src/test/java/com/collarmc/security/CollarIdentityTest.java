package com.collarmc.security;

import com.collarmc.api.identity.ServerIdentity;
import com.collarmc.security.messages.CollarSodium;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class CollarIdentityTest {
    private final CollarSodium collarSodium = new CollarSodium();

    @Test
    public void roundTripClientIdentity() throws Exception {
        ServerIdentity serverIdentity = new ServerIdentity(UUID.randomUUID(), new PublicKey(TokenGenerator.byteToken(16)));
        UUID profile = UUID.randomUUID();
        CollarSodium collarSodium = new CollarSodium();
        CollarIdentity identity = CollarIdentity.createClientIdentity(profile, serverIdentity, collarSodium);
        byte[] identityBytes = identity.serialize();
        CollarIdentity newIdentity = CollarIdentity.from(identityBytes);
        Assert.assertEquals(identity.id, newIdentity.id);
        Assert.assertEquals(identity.serverIdentity, newIdentity.serverIdentity);
        Assert.assertEquals(identity.publicKey(), newIdentity.publicKey());
        Assert.assertArrayEquals(identity.keyPair.getPublicKey().getAsBytes(), newIdentity.keyPair.getPublicKey().getAsBytes());
        Assert.assertArrayEquals(identity.keyPair.getSecretKey().getAsBytes(), newIdentity.keyPair.getSecretKey().getAsBytes());
    }

    @Test
    public void roundTripServerIdentity() throws Exception {
        CollarIdentity identity = CollarIdentity.createServerIdentity(collarSodium);
        byte[] identityBytes = identity.serialize();
        CollarIdentity newIdentity = CollarIdentity.from(identityBytes);
        Assert.assertEquals(identity.id, newIdentity.id);
        Assert.assertEquals(identity.serverIdentity, newIdentity.serverIdentity);
        Assert.assertEquals(identity.publicKey(), newIdentity.publicKey());
        Assert.assertArrayEquals(identity.publicKey().key, newIdentity.publicKey().key);
        Assert.assertArrayEquals(identity.keyPair.getPublicKey().getAsBytes(), newIdentity.keyPair.getPublicKey().getAsBytes());
        Assert.assertArrayEquals(identity.keyPair.getSecretKey().getAsBytes(), newIdentity.keyPair.getSecretKey().getAsBytes());
    }
}
