package com.collarmc.security;

import com.collarmc.api.identity.ServerIdentity;
import com.collarmc.security.messages.CollarSodium;
import com.collarmc.security.messages.SodiumCipher;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class CollarIdentityTest {
    @Test
    public void roundTripClientIdentity() throws Exception {
        CollarSodium sodium = new CollarSodium(false);
        ServerIdentity serverIdentity = new ServerIdentity(UUID.randomUUID(), new PublicKey(TokenGenerator.byteToken(16)));
        UUID profile = UUID.randomUUID();
        CollarIdentity identity = CollarIdentity.createClientIdentity(profile, serverIdentity, sodium);
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
        CollarSodium sodium = new CollarSodium(false);
        CollarIdentity identity = CollarIdentity.createServerIdentity(sodium);
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
