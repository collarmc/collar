package com.collarmc.security.messages;

import com.collarmc.api.identity.ServerIdentity;
import com.collarmc.security.CollarIdentity;
import com.collarmc.security.PublicKey;
import com.collarmc.security.TokenGenerator;
import com.google.crypto.tink.config.TinkConfig;
import org.junit.Assert;
import org.junit.Test;

import java.security.GeneralSecurityException;
import java.util.UUID;

public class CollarIdentityTest {
    @Test
    public void roundTripClientIdentity() throws Exception {
        ServerIdentity serverIdentity = new ServerIdentity(new PublicKey(TokenGenerator.byteToken(16)), new PublicKey(TokenGenerator.byteToken(16)), UUID.randomUUID());
        UUID profile = UUID.randomUUID();
        CollarIdentity identity = CollarIdentity.createClientIdentity(profile, serverIdentity);
        byte[] identityBytes = identity.serialize();
        identity = new CollarIdentity(identityBytes);
        Assert.assertEquals(profile, identity.id);
        Assert.assertEquals(serverIdentity, identity.serverIdentity);
    }

    @Test
    public void roundTripServerIdentity() throws Exception {
        CollarIdentity identity = CollarIdentity.createServerIdentity();
        UUID id = identity.id;
        byte[] identityBytes = identity.serialize();
        identity = new CollarIdentity(identityBytes);
        Assert.assertEquals(id, identity.id);
        Assert.assertNull(identity.serverIdentity);
    }

    static {
        try {
            TinkConfig.init();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
