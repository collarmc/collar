package com.collarmc.security;

import com.collarmc.api.identity.Identity;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.whispersystems.libsignal.util.Hex;

import java.util.Arrays;

/**
 * Represents a {@link Identity}'s public key
 */
public final class PublicKey {
    @JsonProperty("b")
    public final byte[] key;

    public PublicKey(@JsonProperty("k") byte[] key) {
        this.key = key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublicKey publicKey = (PublicKey) o;
        return Arrays.equals(key, publicKey.key);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }

    @Override
    public String toString() {
        return Hex.toString(key);
    }
}
