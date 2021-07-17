package com.collarmc.client.api.textures;

import com.collarmc.client.utils.Http;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.collarmc.api.session.Player;
import com.collarmc.api.textures.TextureType;
import com.collarmc.http.Request;
import com.collarmc.http.Response;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/**
 * Client representation of a remote Texture
 */
public final class Texture {

    private final Logger LOGGER = LogManager.getLogger(Texture.class.getName());

    public final Player player;
    public final UUID group;
    public final TextureType type;
    private final URL url;

    public Texture(Player player, UUID group, TextureType type, URL url) {
        this.player = player;
        this.group = group;
        this.type = type;
        this.url = url;
    }

    /**
     * Async loads the remote texture as a {@link BufferedImage}
     * @param onLoad accepts null if error or image if successful
     */
    public void loadImage(Consumer<Optional<BufferedImage>> onLoad) {
        ForkJoinPool.commonPool().submit(() -> {
            try {
                byte[] bytes = Http.client().execute(Request.url(url).get(), Response.bytes());
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                onLoad.accept(Optional.of(image));
            } catch (Throwable e) {
                LOGGER.error("Failed to load texture from " + url, e);
                onLoad.accept(Optional.empty());
            }
        });
    }
}
