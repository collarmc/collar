package team.catgirl.collar.api.friends;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public final class Friend {
    @JsonProperty("id")
    public final UUID id;
    @JsonProperty("status")
    public final Status status;
    /** Note: not using MinecraftPlayer here as it would leak server ip info to friends */
    @JsonProperty("playerIds")
    public final List<UUID> playerIds;

    public Friend(@JsonProperty("id") UUID id,
                  @JsonProperty("status") Status status,
                  @JsonProperty("playerIds") List<UUID> playerIds) {
        this.id = id;
        this.status = status;
        this.playerIds = playerIds;
    }
}
