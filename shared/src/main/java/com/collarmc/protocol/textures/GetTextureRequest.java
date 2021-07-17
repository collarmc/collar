package com.collarmc.protocol.textures;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.api.textures.TextureType;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.security.ClientIdentity;

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

    public GetTextureRequest(@JsonProperty("identity") ClientIdentity identity,
                             @JsonProperty("player") UUID player,
                             @JsonProperty("group") UUID group,
                             @JsonProperty("type") TextureType type) {
        super(identity);
        this.player = player;
        this.group = group;
        this.type = type;
    }
}
