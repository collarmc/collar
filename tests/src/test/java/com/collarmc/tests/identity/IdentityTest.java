package com.collarmc.tests.identity;

import org.junit.Assert;
import org.junit.Test;
import com.collarmc.client.Collar;
import com.collarmc.client.api.identity.IdentityApi;
import com.collarmc.client.api.identity.IdentityListener;
import com.collarmc.client.security.ClientIdentityStore;
import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.tests.junit.CollarTest;

import java.nio.charset.StandardCharsets;
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

    @Test
    public void createTrust() throws Exception {
        IdentityListenerImpl aliceListener = new IdentityListenerImpl();
        alicePlayer.collar.identities().subscribe(aliceListener);

        IdentityListenerImpl bobListener = new IdentityListenerImpl();
        bobPlayer.collar.identities().subscribe(bobListener);

        // Map from bobs player to his identity
        Optional<ClientIdentity> bobIdentity = alicePlayer.collar.identities().identify(bobPlayerId).get();

        // Start setting up bidirectional trust
        Optional<ClientIdentity> trustedIdentity = alicePlayer.collar.identities().createTrust(bobIdentity).get();
        Assert.assertEquals(trustedIdentity.get(), bobIdentity.get());

        // Trust was exchanged
        Assert.assertEquals(bobPlayer.collar.identity(), aliceListener.lastIdentityTrusted);
        Assert.assertEquals(alicePlayer.collar.identity(), bobListener.lastIdentityTrusted);

        // Lets make sure everyone trusts each other
        Assert.assertNotEquals(aliceListener.lastIdentityTrusted, bobListener.lastIdentityTrusted);
        Assert.assertTrue("Bob should be trusted by Alice", aliceListener.identityStore.isTrustedIdentity(bobPlayer.collar.identity()));
        Assert.assertTrue("Alice should be trusted by Bob", bobListener.identityStore.isTrustedIdentity(alicePlayer.collar.identity()));

        // Round trip some bytes so we know we are all setup :)
        byte[] bobBytes = bobListener.identityStore.cipher().encrypt("UwU".getBytes(StandardCharsets.UTF_8), alicePlayer.collar.identity());
        String messageFromBob = new String(aliceListener.identityStore.cipher().decrypt(bobBytes, bobPlayer.collar.identity()), StandardCharsets.UTF_8);
        Assert.assertEquals("UwU", messageFromBob);

        byte[] aliceBytes = aliceListener.identityStore.cipher().encrypt("OwO".getBytes(StandardCharsets.UTF_8), bobPlayer.collar.identity());
        String messageFromAlice = new String(bobListener.identityStore.cipher().decrypt(aliceBytes, alicePlayer.collar.identity()), StandardCharsets.UTF_8);
        Assert.assertEquals("OwO", messageFromAlice);
    }

    public static final class IdentityListenerImpl implements IdentityListener {

        public ClientIdentity lastIdentityTrusted;
        public ClientIdentityStore identityStore;

        @Override
        public void onIdentityTrusted(Collar collar, IdentityApi api, ClientIdentityStore identityStore, ClientIdentity identity) {
            this.lastIdentityTrusted = identity;
            this.identityStore = identityStore;
            Assert.assertTrue(identity + " was not trusted in " + collar.identity() + "'s store", identityStore.isTrustedIdentity(identity));
        }
    }
}
