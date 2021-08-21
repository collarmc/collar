package com.collarmc.api.session;

import com.collarmc.api.identity.ClientIdentity;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.security.mojang.MinecraftPlayer;

import java.util.Objects;

/**
 * Represents a Collar player
 */
public final class Player {
    @JsonProperty("identity")
    public final ClientIdentity identity;
    @JsonProperty("minecraftPlayer")
    public final MinecraftPlayer minecraftPlayer;

    public Player(@JsonProperty("identity") ClientIdentity identity,
                  @JsonProperty("minecraftPlayer") MinecraftPlayer minecraftPlayer) {
        this.identity = identity;
        this.minecraftPlayer = minecraftPlayer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return identity.profile.equals(player.identity.profile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identity.profile);
    }

    @Override
    public String toString() {
        return identity.profile + ":" + minecraftPlayer;
    }
}
