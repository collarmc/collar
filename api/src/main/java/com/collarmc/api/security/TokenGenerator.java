package com.collarmc.api.security;

import com.google.common.io.BaseEncoding;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class TokenGenerator {

    private static final SecureRandom SECURERANDOM;

    static {
        try {
            SECURERANDOM = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] byteToken(int size) {
        byte[] bytes = new byte[size];
        SECURERANDOM.nextBytes(bytes);
        return bytes;
    }

    public static long longToken() {
        return SECURERANDOM.nextLong();
    }

    /**
     * @return new url token
     */
    public static String urlToken() {
        return BaseEncoding.base64Url().encode(byteToken(16)).replace("==", "");
    }

    private TokenGenerator() {}
}
