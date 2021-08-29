package com.collarmc.client.api.textures.events;

import com.collarmc.client.api.textures.Texture;

/**
 * Fired when a requested texture was received
 */
public final class TextureReceivedEvent {
    public final Texture texture;

    public TextureReceivedEvent(Texture texture) {
        this.texture = texture;
    }
}
