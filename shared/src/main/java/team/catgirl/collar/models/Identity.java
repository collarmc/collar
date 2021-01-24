package team.catgirl.collar.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public final class Identity {

    @JsonProperty("id")
    public final String id;
    @JsonProperty("player")
    public final UUID player;

    private Identity(@JsonProperty("id") String id, @JsonProperty("player") UUID player) {
        this.id = id;
        this.player = player;
    }

    /**
     * Create a unique identity based on the minecraft sessionId and player UUID
     * @param sessionId of the minecraft client
     * @param playerId of the players id
     * @return identity for use over network
     */
    public static Identity from(String sessionId, UUID playerId) {
        if (sessionId == null) throw new IllegalArgumentException("sessionId not supplied");
        if (playerId == null) throw new IllegalArgumentException("playerId not supplied");
        byte[] bytes = Hashing.sha512().hashString(sessionId + playerId.toString(), StandardCharsets.UTF_8).asBytes();
        return new Identity(BaseEncoding.base64().encode(bytes), playerId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Identity identity = (Identity) o;
        return id.equals(identity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
