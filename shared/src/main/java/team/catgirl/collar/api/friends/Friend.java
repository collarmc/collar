package team.catgirl.collar.api.friends;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

/**
 * Represents a Friend
 */
public final class Friend {
    /** The profile that owns this record */
    @JsonProperty("owner")
    public final UUID owner;

    /**
     * Who the owner is friends with
     */
    @JsonProperty("friend")
    public final UUID friend;

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
    public final List<UUID> playerIds;

    @JsonCreator
    public Friend(@JsonProperty("owner") UUID owner,
                  @JsonProperty("friendsWith") UUID friend,
                  @JsonProperty("status") Status status,
                  @JsonProperty("playerIds") List<UUID> playerIds) {
        this.owner = owner;
        this.friend = friend;
        this.status = status;
        this.playerIds = playerIds;
    }
}
