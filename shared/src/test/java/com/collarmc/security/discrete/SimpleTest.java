package com.collarmc.security.discrete;

import com.collarmc.security.Identity;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class SimpleTest {
//
//    @Test
//    public void aliceAndBob() throws CipherException {
//        IdentityStoreImpl aliceStore = new IdentityStoreImpl();
//        IdentityStoreImpl bobStore = new IdentityStoreImpl();
//        IdentityStoreImpl eveStore = new IdentityStoreImpl();
//
//        Cipher aliceCipher = new Cipher(aliceStore);
//        Cipher bobCipher = new Cipher(bobStore);
//        Cipher eveCipher = new Cipher(eveStore);
//
//        // Bob and Alice
//        Identity bob = bobStore.identity();
//        Identity alice = aliceStore.identity();
//        Identity eve = eveStore.identity();
//
//        // Trust each other
//        aliceStore.trust(bob);
//        aliceStore.trust(eve);
//        bobStore.trust(alice);
//        bobStore.trust(eve);
//        eveStore.trust(bob);
//        eveStore.trust(alice);
//
//        // Encrypt the message
//        byte[] encryptForBob = aliceCipher.encrypt("Hello World".getBytes(StandardCharsets.UTF_8), bob);
//
//        // Now decrypt it
//        byte[] decryptedForBob = bobCipher.decrypt(encryptForBob, alice);
//
//        Assert.assertEquals("Hello World", new String(decryptedForBob, StandardCharsets.UTF_8));
//
//        // Eve tries to decrypt Bob's message
//        try {
//            eveCipher.decrypt(encryptForBob, bob);
//            Assert.fail("Eve shouldn't be able to decrypt");
//        } catch (CipherException ignored) {}

//        // Eve tries to be more naughty
//        HybridDecrypt eveHybridDecrypt = aliceStore.messagingPrivateKey.getPrimitive(HybridDecrypt.class);
//        try {
//            eveHybridDecrypt.decrypt(encryptForBob, bob.messageKey);
//            Assert.fail("Eve shouldn't be able to decrypt");
//        } catch (CipherException ignored) {}
//    }
}
