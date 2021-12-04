package com.collarmc.utils;

import com.google.common.io.BaseEncoding;

public final class Hex {
    /**
     * Convert bytes to hex string
     * @param bytes to convert
     * @return hex representation
     */
    public static String hexString(byte[] bytes) {
        return BaseEncoding.base16().lowerCase().encode(bytes);
    }

    private Hex() {}
}
