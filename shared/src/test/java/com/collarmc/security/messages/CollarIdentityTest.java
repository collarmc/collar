package com.collarmc.security.messages;

import com.collarmc.security.CollarIdentity;
import com.google.crypto.tink.config.TinkConfig;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

public class CollarIdentityTest {
    @Test
    public void roundTrip() throws Exception {
        CollarIdentity identity = new CollarIdentity();
        byte[] identityBytes = identity.serialize();
        identity = new CollarIdentity(identityBytes);
    }

    static {
        try {
            TinkConfig.init();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
