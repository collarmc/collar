package com.collarmc.security.jce;

import com.collarmc.security.TokenGenerator;
import com.collarmc.security.messages.CipherException;
import org.junit.Test;

import java.security.KeyPair;

import static org.junit.Assert.fail;

public class JCECipherTest {
    @Test
    public void signatureTest() throws CipherException {
        KeyPair keyPair = JCECipher.generateKeyPair();
        byte[] bytes = TokenGenerator.byteToken(256);
        byte[] sig = JCECipher.sign(bytes, keyPair.getPrivate());
        for (int i = 0; i < bytes.length; i++) {
            try {
                bytes[i]++;
                JCECipher.verify(bytes, sig, keyPair.getPublic());
                fail("CipherException was not thrown");
            } catch (CipherException ex) {
                continue;
            } finally {
                bytes[i]--;
            }
        }
    }
}
