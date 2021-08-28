package com.collarmc.security.messages;

import com.collarmc.security.CollarIdentity;
import com.collarmc.security.TokenGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.Assert.fail;

public class SodiumCipherTest {

    CollarIdentity server;
    CollarIdentity bob;
    Cipher bobCipher;
    CollarIdentity alice;
    Cipher aliceCipher;

    @Before
    public void setup() throws Exception {
        server = CollarIdentity.createServerIdentity();
        bob = CollarIdentity.createClientIdentity(UUID.randomUUID(), server.serverIdentity);
        bobCipher = new SodiumCipher(bob.keyPair);
        alice = CollarIdentity.createClientIdentity(UUID.randomUUID(), server.serverIdentity);
        aliceCipher = new SodiumCipher(alice.keyPair);
    }

    @Test
    public void bobEncryptsDataForHimself() throws Exception {
        byte[] bytes = bobCipher.encrypt("hello world".getBytes(StandardCharsets.UTF_8));
        byte[] plainText = bobCipher.decrypt(bytes, bob.publicKey());
        Assert.assertEquals("hello world", new String(plainText, StandardCharsets.UTF_8));
    }

    @Test
    public void bobSendsMessageToAlice() throws Exception {
        byte[] message = TokenGenerator.byteToken(256);
        byte[] cipherText = bobCipher.encrypt(message, alice.publicKey());
        byte[] decrypted = aliceCipher.decrypt(cipherText, bob.publicKey());
        Assert.assertArrayEquals(message, decrypted);
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
