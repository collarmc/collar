package com.collarmc.security;

import com.collarmc.api.identity.ServerIdentity;
import com.collarmc.api.security.PublicKey;
import com.collarmc.api.security.TokenGenerator;
import com.collarmc.security.messages.CipherException;
import com.collarmc.security.sodium.Sodium;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

public class CollarIdentityTest {
    private static Sodium sodium;

    @BeforeClass
    public static void setupSodium() throws CipherException {
        sodium = Sodium.create();
    }

    @Test
    public void roundTripClientIdentity() throws Exception {
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
