package com.collarmc.server.services.location;

import com.collarmc.api.groups.MemberSource;

import java.util.Objects;
import java.util.Set;

/**
 * Represents players that appear in each others entity lists that should be placed in a group together
 */
public final class NearbyGroup {
    public final Set<MemberSource> players;

    public NearbyGroup(Set<MemberSource> players) {
        this.players = players;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NearbyGroup that = (NearbyGroup) o;
        return players.equals(that.players);
    }

    @Override
    public int hashCode() {
        return Objects.hash(players);
    }
}
