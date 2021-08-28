package com.collarmc.security;

import com.collarmc.api.identity.ServerIdentity;
import com.collarmc.security.CollarIdentity;
import com.collarmc.security.PublicKey;
import com.collarmc.security.TokenGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class CollarIdentityTest {
    @Test
    public void roundTripClientIdentity() throws Exception {
        ServerIdentity serverIdentity = new ServerIdentity(UUID.randomUUID(), new PublicKey(TokenGenerator.byteToken(16)));
        UUID profile = UUID.randomUUID();
        CollarIdentity identity = CollarIdentity.createClientIdentity(profile, serverIdentity);
        byte[] identityBytes = identity.serialize();
        identity = CollarIdentity.from(identityBytes);
        Assert.assertEquals(profile, identity.id);
        Assert.assertEquals(serverIdentity, identity.serverIdentity);
    }

    @Test
    public void roundTripServerIdentity() throws Exception {
        CollarIdentity identity = CollarIdentity.createServerIdentity();
        UUID id = identity.id;
        byte[] identityBytes = identity.serialize();
        identity = CollarIdentity.from(identityBytes);
        Assert.assertEquals(id, identity.id);
        Assert.assertNull(identity.serverIdentity);
    }
}
