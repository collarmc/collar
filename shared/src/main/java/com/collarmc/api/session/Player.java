package com.collarmc.api.session;

import com.collarmc.api.friends.Status;
import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.security.mojang.MinecraftPlayer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Represents a Collar player
 */
public final class Player {
    @JsonProperty("identity")
    @Nonnull
    public final ClientIdentity identity;
    @JsonProperty("minecraftPlayer")
    @Nullable
    public final MinecraftPlayer minecraftPlayer;
    @Nonnull
    @JsonIgnore
    public final Status status;

    public Player(@Nonnull @JsonProperty("identity") ClientIdentity identity,
                  @Nullable @JsonProperty("minecraftPlayer") MinecraftPlayer minecraftPlayer) {
        this.identity = identity;
        this.minecraftPlayer = minecraftPlayer;
        this.status = minecraftPlayer == null ? Status.OFFLINE : Status.ONLINE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return identity.id().equals(player.identity.id());
    }

    @Override
    public int hashCode() {
        return Objects.hash(identity.id());
    }

    @Override
    public String toString() {
        String player = minecraftPlayer == null ? "<none>" : minecraftPlayer.toString();
        return identity.id() + ":" + player;
    }
}
