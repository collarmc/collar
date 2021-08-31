package com.collarmc.security.messages;

import com.google.common.io.ByteStreams;
import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;
import com.goterl.lazysodium.exceptions.SodiumException;
import com.goterl.lazysodium.utils.KeyPair;
import com.goterl.lazysodium.utils.LibraryLoader;
import com.sun.jna.Platform;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

public class CollarSodium {

    private final LazySodiumJava sodium;

    public CollarSodium(boolean server) {
        this.sodium = loadLibrary(server);
    }

    public LazySodiumJava getSodium() {
        return sodium;
    }

    public KeyPair generateKeyPair() throws CipherException {
        try {
            return sodium.cryptoBoxKeypair();
        } catch (SodiumException e) {
            throw new CipherException("Could not generate key pair", e);
        }
    }

    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    private LazySodiumJava loadLibrary(boolean server) {
        if (server && Platform.is64Bit() && Platform.isLinux()) {
            File file = new File("/usr/lib/x86_64-linux-gnu/libsodium.so.23");
            if (!file.exists()) {
                throw new IllegalStateException("libsodium is not installed");
            }
            return new LazySodiumJava(new SodiumJava(file.getAbsolutePath()));
        } else {
            String path = LibraryLoader.getSodiumPathInResources();
            File lib;
            try {
                lib = File.createTempFile("sodium", ".lib");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try (InputStream is = SodiumCipher.class.getResourceAsStream("/" + path);
                 FileOutputStream os = new FileOutputStream(lib)) {
                if (is == null) {
                    throw new IllegalStateException("could not find " + path);
                }
                ByteStreams.copy(is, os);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return new LazySodiumJava(new SodiumJava(lib.getAbsolutePath()));
        }
    }

    public static class CollarSodiumJava extends SodiumJava {
        public CollarSodiumJava(String absolutePath) {
            super(absolutePath);
            new LibraryLoader(Collections.singletonList(CollarSodiumJava.class)).loadAbsolutePath(absolutePath);
        }
    }
}
