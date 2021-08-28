package com.collarmc.api.identity;

import com.collarmc.security.PublicKey;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifies the server
 */
public final class ServerIdentity implements Identity {
    @JsonProperty("publicKey")
    private final PublicKey publicKey;
    @JsonProperty("id")
    private final UUID id;

    public ServerIdentity(@JsonProperty("id") UUID id, @JsonProperty("publicKey") PublicKey publicKey) {
        this.id = id;
        this.publicKey = publicKey;
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public PublicKey publicKey() {
        return publicKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerIdentity that = (ServerIdentity) o;
        return publicKey.equals(that.publicKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicKey);
    }

    @Override
    public String toString() {
        return id().toString();
    }
}
