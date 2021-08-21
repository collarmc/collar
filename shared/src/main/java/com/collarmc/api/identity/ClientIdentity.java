package com.collarmc.api.identity;

import com.collarmc.security.PublicKey;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifies a client
 */
public final class ClientIdentity implements Identity {

    @JsonProperty("profile")
    public final UUID profile;
    @JsonProperty("publicKey")
    public final PublicKey publicKey;
    @JsonProperty("signatureKey")
    public final PublicKey signatureKey;

    public ClientIdentity(@JsonProperty("profile") UUID profile,
                          @JsonProperty("publicKey") PublicKey publicKey,
                          @JsonProperty("signatureKey") PublicKey signatureKey) {
        this.profile = profile;
        this.publicKey = publicKey;
        this.signatureKey = signatureKey;
    }

    @Override
    public PublicKey publicKey() {
        return publicKey;
    }

    @Override
    public PublicKey signatureKey() {
        return signatureKey;
    }

    @Override
    public UUID id() {
        return profile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientIdentity that = (ClientIdentity) o;
        return profile.equals(that.profile) &&
                publicKey.equals(that.publicKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profile, publicKey);
    }

    @Override
    public String toString() {
        return id().toString();
    }
}
