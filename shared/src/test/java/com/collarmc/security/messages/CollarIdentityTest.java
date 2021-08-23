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
    public void roundTrip() throws Exception {
        ServerIdentity serverIdentity = new ServerIdentity(new PublicKey(TokenGenerator.byteToken(16)), new PublicKey(TokenGenerator.byteToken(16)), UUID.randomUUID());
        UUID profileId = UUID.randomUUID();
        CollarIdentity identity = new CollarIdentity(profileId, serverIdentity);
        byte[] identityBytes = identity.serialize();
        identity = new CollarIdentity(identityBytes);
        Assert.assertEquals(profileId, identity.profile);
        Assert.assertEquals(serverIdentity, identity.serverIdentity);
    }

    static {
        try {
            TinkConfig.init();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
