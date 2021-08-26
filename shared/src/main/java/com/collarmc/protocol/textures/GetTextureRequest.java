package com.collarmc.protocol.textures;

import com.collarmc.api.textures.TextureType;
import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Sent when the client requests the texture
 */
public final class GetTextureRequest extends ProtocolRequest {
    @JsonProperty("player")
    public final UUID player;
    @JsonProperty("group")
    public final UUID group;
    @JsonProperty("type")
    public final TextureType type;

    public GetTextureRequest(@JsonProperty("player") UUID player,
                             @JsonProperty("group") UUID group,
                             @JsonProperty("type") TextureType type) {
        this.player = player;
        this.group = group;
        this.type = type;
    }
}
