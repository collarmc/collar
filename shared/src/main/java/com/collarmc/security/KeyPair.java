package com.collarmc.security;

public final class KeyPair {
    private final Key secretKey;
    private final Key publicKey;

    public KeyPair(Key publicKey, Key secretKey) {
        this.publicKey = publicKey;
        this.secretKey = secretKey;
    }

    public Key getSecretKey() {
        return secretKey;
    }

    public Key getPublicKey() {
        return publicKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KeyPair keyPair = (KeyPair) o;

        if (!secretKey.equals(keyPair.secretKey)) return false;
        return publicKey.equals(keyPair.publicKey);
    }

    @Override
    public int hashCode() {
        int result = secretKey.hashCode();
        result = 31 * result + publicKey.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("KeyPair{");
        sb.append("secretKey=").append(secretKey);
        sb.append(", publicKey=").append(publicKey);
        sb.append('}');
        return sb.toString();
    }
}