package com.collarmc.tests.identity;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.tests.junit.CollarTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;
import java.util.UUID;

public class IdentityTest extends CollarTest {
    @Test
    public void getIdentityForCollarPlayer() throws Exception {
        Optional<ClientIdentity> bobIdentity = alicePlayer.collar.identities().identify(bobPlayerId).get();
        Assert.assertEquals(bobIdentity.get(), bobPlayer.collar.identity());
    }

    @Test
    public void getIdentityForNonCollarPlayer() throws Exception {
        Optional<ClientIdentity> bobIdentity = alicePlayer.collar.identities().identify(UUID.randomUUID()).get();
        Assert.assertTrue(bobIdentity.isEmpty());
    }
}
