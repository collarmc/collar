package team.catgirl.collar.security.mojang;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Suppliers;
import com.google.common.io.BaseEncoding;
import io.mikael.urlbuilder.UrlBuilder;
import sun.security.provider.X509Factory;
import team.catgirl.collar.api.http.HttpException;
import team.catgirl.collar.http.HttpClient;
import team.catgirl.collar.http.Request;
import team.catgirl.collar.http.Response;
import team.catgirl.collar.utils.Utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Mojang {

    private static final Logger LOGGER = Logger.getLogger(Mojang.class.getName());

    private static final Supplier<KeyPair> SERVER_KEY_PAIR = Suppliers.memoize(Mojang::createKeyPair);

    private final HttpClient http;
    private final String baseUrl;

    public Mojang(HttpClient http, String baseUrl) {
        this.http = http;
        this.baseUrl = baseUrl;
    }

    public PlayerProfile getProfile(UUID id) {
        String profileId = id.toString().replace("-", "");
        return http.execute(Request.url(baseUrl + "session/minecraft/profile/" + profileId).get(), Response.json(PlayerProfile.class));
    }

    public Optional<JoinServerResponse> joinServer(MinecraftSession session, String serverPublicKey, byte[] sharedSecret) {
        try {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
            md.update("".getBytes(StandardCharsets.ISO_8859_1));
            md.update(sharedSecret);
            md.update(serverPublicKey.getBytes(StandardCharsets.ISO_8859_1));
            byte[] digest = md.digest();
            String serverId = new BigInteger(digest).toString(16);
            JoinRequest joinReq = new JoinRequest(Agent.MINECRAFT, session.accessToken, toProfileId(session.id), serverId);
            http.execute(Request.url(baseUrl + "session/minecraft/join").postJson(joinReq), Response.noContent());
            return Optional.of(new JoinServerResponse(serverId));
        } catch (HttpException e) {
            LOGGER.log(Level.SEVERE, "Could not start verification with Mojang (" + e.code + ")", e);
            return Optional.empty();
        }
    }

    /**
     * Verify that the client can login to the server
     * @param session to check
     * @param serverId server id
     * @return client verified or not
     */
    public boolean hasJoined(MinecraftSession session, String serverId) {
        try {
            UrlBuilder builder = UrlBuilder.fromString(baseUrl + "session/minecraft/hasJoined")
                    .addParameter("username", session.username)
                    .addParameter("serverId", serverId);
            HasJoinedResponse hasJoinedResponse = http.execute(Request.url(builder).get(), Response.json(HasJoinedResponse.class));
            return hasJoinedResponse.id.equals(toProfileId(session.id));
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, "Couldn't verify " + session.username,e);
            return false;
        }
    }

    /**
     * Validates the clients access token
     * @param accessToken to test
     * @param clientToken to test
     * @return accessToken is valid or not
     */
    public boolean validateToken(String accessToken, String clientToken) {
        try {
            http.execute(Request.url(baseUrl + "session/minecraft/validate").postJson(new ValidateTokenRequest(accessToken, clientToken)), Response.noContent());
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Validates the clients access token
     * @param request to send
     * @return accessToken is valid or not
     */
    public Optional<RefreshTokenResponse> refreshToken(RefreshTokenRequest request) {
        try {
            return Optional.of(http.execute(Request.url(baseUrl + "session/minecraft/refresh").postJson(request), Response.json(RefreshTokenResponse.class)));
        } catch (Throwable e) {
            return Optional.empty();
        }
    }


    public static final class RefreshTokenRequest {
        @JsonProperty("accessToken")
        public final String accessToken;
        @JsonProperty("clientToken")
        public final String clientToken;
        @JsonProperty("selectedProfile")
        public final SelectedProfile selectedProfile;
        @JsonProperty("requestUser")
        public final boolean requestUser;

        public RefreshTokenRequest(@JsonProperty("accessToken") String accessToken,
                                   @JsonProperty("clientToken") String clientToken,
                                   @JsonProperty("selectedProfile") SelectedProfile selectedProfile,
                                   @JsonProperty("requestUser") boolean requestUser) {
            this.accessToken = accessToken;
            this.clientToken = clientToken;
            this.selectedProfile = selectedProfile;
            this.requestUser = requestUser;
        }
    }

    public static final class RefreshTokenResponse {
        @JsonProperty("accessToken")
        public final String accessToken;
        @JsonProperty("clientToken")
        public final String clientToken;
        @JsonProperty("selectedProfile")
        public final SelectedProfile selectedProfile;
        @JsonProperty("user")
        public final PlayerProfile user;

        public RefreshTokenResponse(@JsonProperty("accessToken") String accessToken,
                                    @JsonProperty("clientToken") String clientToken,
                                    @JsonProperty("selectedProfile") SelectedProfile selectedProfile,
                                    @JsonProperty("user") PlayerProfile user) {
            this.accessToken = accessToken;
            this.clientToken = clientToken;
            this.selectedProfile = selectedProfile;
            this.user = user;
        }
    }

    public static final class SelectedProfile {
        @JsonProperty("id")
        public final String id;
        @JsonProperty("name")
        public final String name;

        public SelectedProfile(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static final class ValidateTokenRequest {
        @JsonProperty("accessToken")
        public final String accessToken;
        @JsonProperty("clientToken")
        public final String clientToken;

        public ValidateTokenRequest(@JsonProperty("accessToken") String accessToken,
                                    @JsonProperty("clientToken") String clientToken) {
            this.accessToken = accessToken;
            this.clientToken = clientToken;
        }
    }

    public static final class PlayerProfile {
        @JsonProperty("id")
        public final String id;
        @JsonProperty("name")
        public final String name;
        @JsonProperty("properties")
        public final List<ProfileProperty> properties;

        public PlayerProfile(@JsonProperty("id") String id,
                             @JsonProperty("name") String name,
                             @JsonProperty("properties") List<ProfileProperty> properties) {
            this.id = id;
            this.name = name;
            this.properties = properties;
        }

        public Optional<TexturesProperty> textures() {
            return properties.stream().filter(profileProperty -> profileProperty.name.equals("textures"))
                    .map(profileProperty -> profileProperty.value)
                    .map(value -> BaseEncoding.base64().decode(value))
                    .map(bytes -> {
                        try {
                            return Utils.jsonMapper().readValue(bytes, TexturesProperty.class);
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .findFirst();
        }
    }

    public static final class ProfileProperty {
        @JsonProperty("name")
        public final String name;
        @JsonProperty("value")
        public final String value;

        public ProfileProperty(@JsonProperty("name") String name,
                               @JsonProperty("value") String value) {
            this.name = name;
            this.value = value;
        }
    }

    public static final class TexturesProperty {
        @JsonProperty("timestamp")
        public final long timestamp;
        @JsonProperty("profileId")
        public final String profileId;
        @JsonProperty("profileName")
        public final String profileName;
        public final Map<String, Texture> textures;

        public TexturesProperty(@JsonProperty("timestamp") long timestamp,
                                @JsonProperty("profileId") String profileId,
                                @JsonProperty("profileName") String profileName,
                                @JsonProperty("textures") Map<String, Texture> textures) {
            this.timestamp = timestamp;
            this.profileId = profileId;
            this.profileName = profileName;
            this.textures = textures;
        }

        public Optional<String> skin() {
            return getTexture("SKIN");
        }

        public Optional<String> cape() {
            return getTexture("CAPE");
        }

        private Optional<String> getTexture(String name) {
            Texture texture = textures.get(name);
            return texture == null ? Optional.empty() : Optional.of(texture.url);
        }
    }

    public static final class Texture {
        @JsonProperty("url")
        public final String url;

        public Texture(@JsonProperty("url") String url) {
            this.url = url;
        }
    }

    public static final class JoinRequest {
        @JsonProperty("agent")
        public final Agent agent;
        @JsonProperty("accessToken")
        public final String accessToken;
        @JsonProperty("selectedProfile")
        public final String selectedProfile;
        @JsonProperty("serverId")
        public final String serverId;

        @JsonCreator
        public JoinRequest(@JsonProperty("agent") Agent agent,
                           @JsonProperty("accessToken") String accessToken,
                           @JsonProperty("selectedProfile") String selectedProfile,
                           @JsonProperty("serverId") String serverId) {
            this.agent = agent;
            this.accessToken = accessToken;
            this.selectedProfile = selectedProfile;
            this.serverId = serverId;
        }
    }

    public static final class HasJoinedResponse {
        @JsonProperty("id")
        public final String id;
        @JsonProperty("name")
        public final String name;

        @JsonCreator
        public HasJoinedResponse(@JsonProperty("id") String id,
                                 @JsonProperty("name") String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static class JoinServerResponse {
        @JsonProperty("serverId")
        public final String serverId;

        public JoinServerResponse(@JsonProperty("serverId") String serverId) {
            this.serverId = serverId;
        }
    }

    public static class Agent {

        public static final Agent MINECRAFT = new Agent("Minecraft", 1);

        @JsonProperty("name")
        public final String name;
        @JsonProperty("version")
        public final Integer version;

        public Agent(@JsonProperty("name") String name,
                     @JsonProperty("version") Integer version) {
            this.name = name;
            this.version = version;
        }
    }

    public static String serverPublicKey() {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        printWriter.println(X509Factory.BEGIN_CERT);
        printWriter.println(BaseEncoding.base32().encode(SERVER_KEY_PAIR.get().getPublic().getEncoded()));
        printWriter.println(X509Factory.END_CERT);
        return writer.toString();
    }

    public static String toProfileId(UUID id) {
        return id.toString().replace("-", "");
    }

    private static KeyPair createKeyPair() {
        KeyPair kp;
        try {
            KeyPairGenerator keyPairGene = KeyPairGenerator.getInstance("RSA");
            keyPairGene.initialize(1024);
            kp = keyPairGene.genKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("problem generating the key", e);
        }
        return kp;
    }
}
