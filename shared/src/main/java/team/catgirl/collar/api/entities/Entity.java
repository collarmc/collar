package team.catgirl.collar.api.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public final class Entity {
    @JsonProperty("id")
    public final Integer id;
    @JsonProperty("playerId")
    public final UUID playerId;
    @JsonProperty("type")
    public final EntityType type;

    public Entity(@JsonProperty("id") Integer id,
                  @JsonProperty("playerId") UUID playerId,
                  @JsonProperty("type") EntityType type) {
        this.id = id;
        this.playerId = playerId;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity entity = (Entity) o;
        return id.equals(entity.id) && Objects.equals(playerId, entity.playerId) && type == entity.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, playerId, type);
    }
}
