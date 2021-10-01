package com.collarmc.security.messages;

import com.collarmc.security.CollarIdentity;
import com.collarmc.security.TokenGenerator;
import com.collarmc.security.sodium.Sodium;
import com.collarmc.security.sodium.SodiumCipher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.fail;

public class SodiumCipherTest {

    CollarIdentity server;
    CollarIdentity bob;
    Cipher bobCipher;
    CollarIdentity alice;
    Cipher aliceCipher;
    CollarIdentity eve;
    Cipher eveCipher;

    @Before
    public void setup() throws Exception {
        final Sodium sodium = Sodium.create();
        server = CollarIdentity.createServerIdentity(sodium);
        bob = CollarIdentity.createClientIdentity(UUID.randomUUID(), server.serverIdentity, sodium);
        bobCipher = new SodiumCipher(sodium, bob.keyPair);
        alice = CollarIdentity.createClientIdentity(UUID.randomUUID(), server.serverIdentity, sodium);
        aliceCipher = new SodiumCipher(sodium, alice.keyPair);
        eve = CollarIdentity.createClientIdentity(UUID.randomUUID(), server.serverIdentity, sodium);
        eveCipher = new SodiumCipher(sodium, eve.keyPair);
    }

    @Test
    public void bobEncryptsDataForHimself() throws Exception {
        byte[] message = "hello world".getBytes(StandardCharsets.UTF_8);
        byte[] cipherText = bobCipher.encrypt(message);
        Assert.assertFalse(Arrays.equals(cipherText, message));
        byte[] plainText = bobCipher.decrypt(cipherText, bob.publicKey());
        Assert.assertEquals("hello world", new String(plainText, StandardCharsets.UTF_8));
    }

    @Test
    public void bobSendsMessageToAlice() throws Exception {
        byte[] message = TokenGenerator.byteToken(256);
        byte[] cipherText = bobCipher.encrypt(message, alice.publicKey());
        Assert.assertFalse(Arrays.equals(cipherText, message));
        byte[] decrypted = aliceCipher.decrypt(cipherText, bob.publicKey());
        Assert.assertArrayEquals(message, decrypted);
    }

    @Test
    public void eveCantReadBobsMessageToAlice() throws Exception {
        byte[] message = TokenGenerator.byteToken(256);
        byte[] cipherText = bobCipher.encrypt(message, alice.publicKey());
        try {
            eveCipher.decrypt(cipherText, bob.publicKey());
            fail("eve could read bobs message to alice!");
        } catch (CipherException ignored) {}

        try {
            eveCipher.decrypt(cipherText, alice.publicKey());
            fail("eve could read bobs message to alice!");
        } catch (CipherException ignored) {}

        try {
            eveCipher.decrypt(cipherText, eve.publicKey());
            fail("eve could read bobs message to alice!");
        } catch (CipherException ignored) {}
    }

    @Test
    public void differentLengthMessages() throws Exception {
        int MAX_SIZE = Short.MAX_VALUE / 2;
        for (int i = 0; i < MAX_SIZE; i++) {
            byte[] token = TokenGenerator.byteToken(i);
            byte[] bytes = bobCipher.encrypt(token, alice.publicKey());
            byte[] plainText = aliceCipher.decrypt(bytes, bob.publicKey());
            Assert.assertArrayEquals(token, plainText);
        }
    }

    @Test
    public void encryptDecryptAndMutate() throws Exception {
        byte[] token = TokenGenerator.byteToken(1024);
        byte[] bytes = bobCipher.encrypt(token);

        // Try to decrypt
        byte[] plainText = bobCipher.decrypt(bytes, bob.publicKey());
        Assert.assertArrayEquals(token, plainText);

        // Mutate the cipherText and try to decrypt
        for (int i = 0; i < bytes.length; i++) {
            try {
                bytes[i]++;
                bobCipher.decrypt(bytes);
                fail("CipherException was not thrown");
            } catch (CipherException ex) {
                continue;
            } finally {
                bytes[i]--;
            }
        }
    }
}
