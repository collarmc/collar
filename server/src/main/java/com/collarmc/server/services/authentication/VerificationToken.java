package com.collarmc.server.services.authentication;

import com.google.common.io.BaseEncoding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Used by the {@link ServerAuthenticationService} for identifying users for password resets, etc.
 */
public final class VerificationToken {

    private static final Logger LOGGER = LogManager.getLogger(VerificationToken.class.getName());

    private static final int VERSION = 1;

    public final UUID profileId;
    public final long expiresAt;

    public VerificationToken(UUID profileId, long expiresAt) {
        this.profileId = profileId;
        this.expiresAt = expiresAt;
    }

    public static Optional<VerificationToken> from(TokenCrypter crypter, String token) {
        byte[] bytes; {
            try {
                bytes = crypter.decrypt(BaseEncoding.base64Url().decode(token));
            } catch (Throwable e) {
                LOGGER.error("Problem decoding token bytes", e);
                return Optional.empty();
            }
        }
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes)) {
            try (DataInputStream objectStream = new DataInputStream(byteStream)) {
                String profileId;
                long expiresAt;
                int version = objectStream.readInt();
                switch (version) {
                    case 1:
                        profileId = objectStream.readUTF();
                        expiresAt = objectStream.readLong();
                        break;
                    default:
                        throw new IllegalStateException("unsupported version " + version);
                }
                Date expiredAt = new Date(expiresAt);
                if (new Date().after(expiredAt)) {
                    LOGGER.info("Token expired at " + expiredAt);
                    return Optional.empty();
                } else {
                    return Optional.of(new VerificationToken(UUID.fromString(profileId), expiresAt));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Decoding token structure", e);
            return Optional.empty();
        }
    }

    public String serialize(TokenCrypter crypter) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            try (DataOutputStream objectStream = new DataOutputStream(byteStream)) {
                objectStream.writeInt(VERSION);
                objectStream.writeUTF(profileId.toString());
                objectStream.writeLong(expiresAt);
            }
            return BaseEncoding.base64Url().encode(crypter.crypt(byteStream.toByteArray()));
        }
    }
}
