package com.collarmc.server.security;


import com.collarmc.security.TokenCrypter;
import org.jasypt.util.binary.AES256BinaryEncryptor;

public class TokenCrypterImpl implements TokenCrypter {

    private final AES256BinaryEncryptor encryptor;

    public TokenCrypterImpl(String password) {
        encryptor = new AES256BinaryEncryptor();
        encryptor.setPassword(password);
    }

    @Override
    public byte[] decrypt(byte[] bytes) {
        return encryptor.decrypt(bytes);
    }

    @Override
    public byte[] crypt(byte[] bytes) {
        return encryptor.encrypt(bytes);
    }
}
