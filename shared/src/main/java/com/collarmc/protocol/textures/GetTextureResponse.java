package com.collarmc.protocol.textures;

import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.security.ServerIdentity;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.api.session.Player;
import com.collarmc.api.textures.TextureType;

import java.util.UUID;

/**
 * Sent back to the client in response to {@link GetTextureRequest}
 */
public final class GetTextureResponse extends ProtocolResponse {
    @JsonProperty("textureId")
    public final UUID textureId;
    @JsonProperty("group")
    public final UUID group;
    @JsonProperty("player")
    public final Player player;
    @JsonProperty("texturePath")
    public final String texturePath;
    @JsonProperty("type")
    public final TextureType type;

    public GetTextureResponse(@JsonProperty("identity") ServerIdentity identity,
                              @JsonProperty("textureId") UUID textureId,
                              @JsonProperty("group") UUID group,
                              @JsonProperty("player") Player player,
                              @JsonProperty("texturePath") String texturePath,
                              @JsonProperty("type") TextureType type) {
        super(identity);
        this.textureId = textureId;
        this.group = group;
        this.player = player;
        this.texturePath = texturePath;
        this.type = type;
    }
}
