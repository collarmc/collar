package com.collarmc.server.security.hashing;

public interface Hasher {
    byte[] hash(byte[] hash);
    String hash(String hash);
}
