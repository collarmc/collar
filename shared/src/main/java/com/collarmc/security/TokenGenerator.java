package com.collarmc.security;

import com.collarmc.utils.Utils;
import com.google.common.io.BaseEncoding;

public final class TokenGenerator {

    public static byte[] byteToken(int size) {
        byte[] bytes = new byte[size];
        Utils.secureRandom().nextBytes(bytes);
        return bytes;
    }

    public static long longToken() {
        return Utils.secureRandom().nextLong();
    }

    /**
     * @return new url token
     */
    public static String urlToken() {
        return BaseEncoding.base64Url().encode(byteToken(16)).replace("==", "");
    }

    private TokenGenerator() {}
}
