package com.collarmc.security.messages;

import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;
import com.goterl.lazysodium.exceptions.SodiumException;
import com.goterl.lazysodium.utils.KeyPair;
import com.goterl.lazysodium.utils.LibraryLoader;

/**
 * Sodium library wrapper class. Handles loading on multiplatform builds.
 */
public class CollarSodium extends LazySodiumJava {

    public CollarSodium() {
        super(new SodiumJava(LibraryLoader.Mode.PREFER_BUNDLED));
    }

    /**
     * Generates a public/private keypair.
     * @return public/private keypair
     * @throws CipherException
     */
    public KeyPair generateKeyPair() throws CipherException {
        try {
            return this.cryptoBoxKeypair();
        } catch (SodiumException e) {
            throw new CipherException("Could not generate key pair", e);
        }
    }
}
