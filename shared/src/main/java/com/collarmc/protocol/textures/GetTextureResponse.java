package com.collarmc.protocol.textures;

import com.collarmc.api.session.Player;
import com.collarmc.api.textures.TextureType;
import com.collarmc.protocol.ProtocolResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * Sent back to the client in response to {@link GetTextureRequest}
 */
public final class GetTextureResponse extends ProtocolResponse {
    private static final Logger LOGGER = LogManager.getLogger(GetTextureResponse.class);
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

    public GetTextureResponse(@JsonProperty("textureId") UUID textureId,
                              @JsonProperty("group") UUID group,
                              @JsonProperty("player") Player player,
                              @JsonProperty("texturePath") String texturePath,
                              @JsonProperty("type") TextureType type) {
        StringBuffer sb = new StringBuffer();
        sb.append("GetTextureResponse constructor:: textureId: ");
        sb.append(player==null ? "null" : player.minecraftPlayer.id.toString());
        sb.append(", group: ");
        sb.append(group == null ? "null" : group.toString());
        sb.append(", type: ");
        sb.append(type == null ? "null" : type.toString());
        sb.append(", texturePath: ");
        sb.append(texturePath == null ? "null" : texturePath);
        LOGGER.info(sb.toString());
        this.textureId = textureId;
        this.group = group;
        this.player = player;
        this.texturePath = texturePath;
        this.type = type;
    }
}
