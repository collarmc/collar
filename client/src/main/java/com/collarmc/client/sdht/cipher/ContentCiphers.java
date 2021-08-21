package com.collarmc.client.sdht.cipher;

import com.collarmc.sdht.Content;
import com.collarmc.sdht.cipher.ContentCipher;
import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.security.messages.CipherException;
import com.collarmc.security.messages.CipherException.UnavailableCipherException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Holds all the available {@link ContentCipher}'s for SDHT name spaces
 */
public final class ContentCiphers implements ContentCipher {
    private final List<ContentCipher> ciphers = new ArrayList<>();

    /**
     * Register a {@link ContentCipher}
     * @param cipher to register
     */
    public void register(ContentCipher cipher) {
        ciphers.add(cipher);
    }

    @Override
    public byte[] crypt(ClientIdentity identity, UUID namespace, Content bytes) throws CipherException {
        ContentCipher cipher = ciphers.stream()
                .filter(recordCipher -> recordCipher.accepts(namespace))
                .findFirst().orElseThrow(() -> new UnavailableCipherException("cannot find cipher for namespace " + namespace));
        return cipher.crypt(identity, namespace, bytes);
    }

    @Override
    public Content decrypt(ClientIdentity identity, UUID namespace, byte[] bytes) throws CipherException {
        ContentCipher cipher = ciphers.stream()
                .filter(recordCipher -> recordCipher.accepts(namespace))
                .findFirst().orElseThrow(() -> new UnavailableCipherException("cannot find cipher for namespace " + namespace));
        return cipher.decrypt(identity, namespace, bytes);
    }

    @Override
    public boolean accepts(UUID namespace) {
        throw new IllegalStateException("not implemented");
    }
}
