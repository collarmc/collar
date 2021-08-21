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
    public final PublicKey publicKey;
    @JsonProperty("signatureKey")
    public final PublicKey signatureKey;
    @JsonProperty("serverId")
    public final UUID serverId;

    public ServerIdentity(@JsonProperty("publicKey") PublicKey publicKey,
                          @JsonProperty("signatureKey") PublicKey signatureKey,
                          @JsonProperty("serverId") UUID serverId) {
        this.publicKey = publicKey;
        this.signatureKey = signatureKey;
        this.serverId = serverId;
    }

    @Override
    public UUID id() {
        return serverId;
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
