package team.catgirl.collar.server.security.hashing;

import at.favre.lib.crypto.bcrypt.BCrypt;

import java.nio.charset.Charset;

public class PasswordHashing {

    private final byte[] salt;

    public PasswordHashing(String salt) {
        this.salt = salt.getBytes();
    }

    public String hash(String input) {
        byte[] hash = BCrypt.withDefaults().hash(12, salt, input.getBytes());
        return new String(hash, Charset.defaultCharset());
    }

    public boolean verify(char[] password, char[] hash) {
        return BCrypt.verifyer().verify(password, hash).verified;
    }
}
