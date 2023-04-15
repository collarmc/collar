package com.collarmc.protocol.textures;

import com.collarmc.api.textures.TextureType;
import com.collarmc.protocol.ProtocolRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * Sent when the client requests the texture
 */
public final class GetTextureRequest extends ProtocolRequest {

    private static final Logger LOGGER = LogManager.getLogger(GetTextureRequest.class);

    @JsonProperty("player")
    public final UUID player;
    @JsonProperty("group")
    public final UUID group;
    @JsonProperty("type")
    public final TextureType type;

    public GetTextureRequest(@JsonProperty("player") UUID player,
                             @JsonProperty("group") UUID group,
                             @JsonProperty("type") TextureType type) {
        StringBuffer sb = new StringBuffer();
        sb.append("GetTextureRequest constructor:: player: ");
        sb.append(player == null ? "null" : player.toString());
        sb.append(", group: ");
        sb.append(group == null ? "null" : group.toString());
        sb.append(", type: ");
        sb.append(type == null ? "null" : type.toString());
        LOGGER.info(sb.toString());
        this.player = player;
        this.group = group;
        this.type = type;
    }
}
