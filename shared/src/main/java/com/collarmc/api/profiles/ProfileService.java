package com.collarmc.api.profiles;

import com.collarmc.api.http.RequestContext;
import com.collarmc.security.PublicKey;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public interface ProfileService {
    CreateProfileResponse createProfile(RequestContext context, CreateProfileRequest req);

    GetProfileResponse getProfile(RequestContext context, GetProfileRequest req);

    UpdateProfileResponse updateProfile(RequestContext context, UpdateProfileRequest req);

    PlayerCountResponse playerCount(RequestContext context, PlayerCountRequest req);

    class PlayerCountRequest {}

    class PlayerCountResponse {
        public final long total;

        public PlayerCountResponse(long total) {
            this.total = total;
        }
    }

    class CreateProfileRequest {
        public final String email;
        public final String password;
        public final String name;

        public CreateProfileRequest(String email, String password, String name) {
            this.email = email;
            this.password = password;
            this.name = name;
        }
    }

    class CreateProfileResponse {
        public final Profile profile;

        public CreateProfileResponse(Profile profile) {
            this.profile = profile;
        }
    }

    class GetProfileRequest {
        public final UUID byId;
        public final String byEmail;

        public GetProfileRequest(UUID byId, String byEmail) {
            this.byId = byId;
            this.byEmail = byEmail;
        }

        public static GetProfileRequest byId(UUID uuid) {
            return new GetProfileRequest(uuid, null);
        }

        public static GetProfileRequest byEmail(String email) {
            return new GetProfileRequest(null, email);
        }
    }

    class GetProfileResponse {
        public final Profile profile;

        public GetProfileResponse(Profile profile) {
            this.profile = profile;
        }
    }

    final class UpdateProfileRequest {
        @JsonProperty("profile")
        public final UUID profile;
        @JsonProperty("emailVerified")
        public final Boolean emailVerified;
        @JsonProperty("hashedPassword")
        public final String hashedPassword;
        @JsonProperty("addMinecraftAccount")
        public final UUID addMinecraftAccount;
        @JsonProperty("cape")
        public final TexturePreference cape;
        @JsonProperty("publicKey")
        public final PublicKey publicKey;

        public UpdateProfileRequest(@JsonProperty("profile") UUID profile,
                                    @JsonProperty("emailVerified") Boolean emailVerified,
                                    @JsonProperty("hashedPassword") String hashedPassword,
                                    @JsonProperty("addMinecraftAccount") UUID addMinecraftAccount,
                                    @JsonProperty("cape") TexturePreference cape,
                                    @JsonProperty("publicKey") PublicKey publicKey) {
            this.profile = profile;
            this.emailVerified = emailVerified;
            this.hashedPassword = hashedPassword;
            this.addMinecraftAccount = addMinecraftAccount;
            this.cape = cape;
            this.publicKey = publicKey;
        }

        public static UpdateProfileRequest emailVerified(UUID profile) {
            return new UpdateProfileRequest(profile, true, null, null, null, null);
        }

        public static UpdateProfileRequest hashedPassword(UUID profile, String newPassword) {
            return new UpdateProfileRequest(profile, null, newPassword, null, null, null);
        }

        public static UpdateProfileRequest keys(UUID profile, PublicKey publicKey) {
            return new UpdateProfileRequest(profile, null, null, null, null, publicKey);
        }

        public static UpdateProfileRequest resetKeys(UUID profile) {
            return keys(profile, new PublicKey(new byte[0]));
        }

        public static UpdateProfileRequest capeTexturePreference(UUID profile, TexturePreference capeTexture) {
            return new UpdateProfileRequest(profile, null, null, null, capeTexture, null);
        }

        public static UpdateProfileRequest addMinecraftAccount(UUID profile, UUID minecraftAccount) {
            return new UpdateProfileRequest(profile, null, null, minecraftAccount, null, null);
        }
    }

    final class UpdateProfileResponse {
        @JsonProperty("profile")
        public final Profile profile;

        public UpdateProfileResponse(@JsonProperty("profile") Profile profile) {
            this.profile = profile;
        }
    }
}
