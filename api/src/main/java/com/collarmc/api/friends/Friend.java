package com.collarmc.api.friends;

import com.collarmc.api.profiles.PublicProfile;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a Friend
 */
public final class Friend {
    /**
     * Who the owner is friends with
     */
    @JsonProperty("profile")
    public final PublicProfile profile;

    /**
     * The current status of the friend
     */
    @JsonProperty("status")
    public final Status status;

    /**
     * The Minecraft player id's of this friend
     * Note: not using MinecraftPlayer here as it would leak server ip info to friends
     */
    @JsonProperty("playerIds")
    public final Set<UUID> playerIds;

    @JsonCreator
    public Friend(@JsonProperty("profile") PublicProfile profile,
                  @JsonProperty("status") Status status,
                  @JsonProperty("playerIds") Set<UUID> playerIds) {
        this.profile = profile;
        this.status = status;
        this.playerIds = playerIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Friend friend = (Friend) o;
        return profile.equals(friend.profile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profile);
    }
}
