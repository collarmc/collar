package com.collarmc.security.messages;

import com.collarmc.security.CollarIdentity;
import com.collarmc.security.TokenGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.Assert.fail;

public class CipherTest {

    @Test
    public void roundTrip() throws Exception {
        CollarIdentity clientIdentity = CollarIdentity.createClientIdentity(UUID.randomUUID(), CollarIdentity.createServerIdentity().serverIdentity);
        Cipher cipher = new SodiumCipher(clientIdentity.keyPair);

        byte[] bytes = cipher.encrypt("hello world".getBytes(StandardCharsets.UTF_8));

        // Try to decrypt
        byte[] plainText = cipher.decrypt(bytes, clientIdentity.publicKey());
        Assert.assertEquals("hello world", new String(plainText, StandardCharsets.UTF_8));
    }

    @Test
    public void encryptDecryptAndMutate() throws Exception {
        CollarIdentity clientIdentity = CollarIdentity.createClientIdentity(UUID.randomUUID(), CollarIdentity.createServerIdentity().serverIdentity);
        Cipher cipher = new SodiumCipher(clientIdentity.keyPair);
        byte[] token = TokenGenerator.byteToken(1024);
        byte[] bytes = cipher.encrypt(token);

        // Try to decrypt
        byte[] plainText = cipher.decrypt(bytes, clientIdentity.publicKey());
        Assert.assertArrayEquals(token, plainText);

        // Mutate the cipherText and try to decrypt
        for (int i = 0; i < bytes.length; i++) {
            try {
                bytes[i]++;
                cipher.decrypt(bytes);
                fail("CipherException was not thrown");
            } catch (CipherException ex) {
                continue;
            } finally {
                bytes[i]--;
            }
        }
    }
}
