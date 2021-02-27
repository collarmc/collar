package team.catgirl.collar.server.http;

import com.google.common.io.BaseEncoding;
import team.catgirl.collar.server.services.authentication.TokenCrypter;

import java.io.*;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used for authorizing API calls
 */
public class ApiToken {

    private static final Logger LOGGER = Logger.getLogger(ApiToken.class.getName());

    private static final int VERSION = 1;

    public final UUID profileId;
    public final long expiresAt;

    public ApiToken(UUID profileId, long expiresAt) {
        this.profileId = profileId;
        this.expiresAt = expiresAt;
    }

    public ApiToken(UUID profileId) {
        this.profileId = profileId;
        this.expiresAt = new Date().getTime() * TimeUnit.HOURS.toMillis(24);
    }

    public boolean isExpired() {
        return new Date().after(new Date(expiresAt));
    }

    public String serialize(TokenCrypter crypter) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (DataOutputStream dataStream = new DataOutputStream(outputStream)) {
                dataStream.write(VERSION);
                dataStream.writeUTF(profileId.toString());
                dataStream.writeLong(expiresAt);
            }
            String token = BaseEncoding.base64Url().encode(crypter.crypt(outputStream.toByteArray()));
            LOGGER.log(Level.INFO, "Created token '" + token + "'");
            return token;
        }
    }

    public static ApiToken deserialize(TokenCrypter crypter, String token) throws IOException {
        LOGGER.log(Level.INFO, "deserialize token '" + token + "'");
        byte[] bytes = BaseEncoding.base64Url().decode(token);
        byte[] decryptedBytes = crypter.decrypt(bytes);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(decryptedBytes)) {
            try (DataInputStream dataStream = new DataInputStream(inputStream)) {
                int version = dataStream.read();
                String uuidAsString;
                long expiresAt;
                switch (version) {
                    case 1:
                        uuidAsString = dataStream.readUTF();
                        expiresAt = dataStream.readLong();
                        break;
                    default:
                        throw new IllegalStateException("unknown version " + version);
                }
                return new ApiToken(UUID.fromString(uuidAsString), expiresAt);
            }
        }
    }

    public RequestContext fromToken() {
        return new RequestContext(profileId);
    }
}
