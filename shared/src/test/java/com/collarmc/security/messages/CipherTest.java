package com.collarmc.security.messages;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.security.CollarIdentity;
import com.collarmc.security.TokenGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.fail;

public class CipherTest {
    @Test
    public void encryptDecryptAndMutate() throws Exception {
        CollarIdentity clientIdentity = CollarIdentity.createClientIdentity(UUID.randomUUID(), CollarIdentity.createServerIdentity().serverIdentity);
        Cipher cipher = new Cipher(clientIdentity);
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
