package com.collarmc.sdht.cipher;

import com.collarmc.sdht.Content;
import com.collarmc.sdht.DistributedHashTable;
import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.security.messages.CipherException;

import java.util.UUID;

/**
 * Cipher for {@link DistributedHashTable} {@link Content}
 */
public interface ContentCipher {
    /**
     * Encrypt a record's bytes
     * @param identity of the owner
     * @param namespace that the bytes will be stored in
     * @param bytes of encrupted content
     * @return bytes
     */
    byte[] crypt(ClientIdentity identity, UUID namespace, Content bytes) throws CipherException;

    /**
     *
     * @param identity of the sender
     * @param namespace the bytes belong to
     * @param bytes to decrypt
     * @return content
     */
    Content decrypt(ClientIdentity identity, UUID namespace, byte[] bytes) throws CipherException;

    /**
     * Accepts decoding of the provided namespace
     * @param namespace to decode
     * @return if decodable or not with this cipher
     */
    boolean accepts(UUID namespace);
}
