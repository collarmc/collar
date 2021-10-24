package com.collarmc.security;

public interface TokenCrypter {
    /** decrypt token **/
    byte[] decrypt(byte[] bytes);
    /** encrypt token **/
    byte[] crypt(byte[] bytes);
}
