package com.collarmc.client.api.textures;

import com.collarmc.api.groups.Group;
import com.collarmc.api.session.Player;
import com.collarmc.api.textures.TextureType;
import com.collarmc.client.Collar;
import com.collarmc.client.api.AbstractApi;
import com.collarmc.client.api.textures.events.TextureReceivedEvent;
import com.collarmc.client.security.ClientIdentityStore;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.protocol.textures.GetTextureRequest;
import com.collarmc.protocol.textures.GetTextureResponse;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.stoyanr.evictor.ConcurrentMapWithTimedEviction;
import com.stoyanr.evictor.map.ConcurrentHashMapWithTimedEviction;
import com.stoyanr.evictor.map.EvictibleEntry;
import com.stoyanr.evictor.scheduler.RegularTaskEvictionScheduler;
import io.mikael.urlbuilder.UrlBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TexturesApi extends AbstractApi {
    private static final Logger LOGGER = LogManager.getLogger(TexturesApi.class);
    private final RegularTaskEvictionScheduler<TextureKey, CompletableFuture<Optional<Texture>>> texturesFutureScheduler = new RegularTaskEvictionScheduler<TextureKey, CompletableFuture<Optional<Texture>>>(1, TimeUnit.SECONDS) {
        @Override
        protected void onScheduleEviction(EvictibleEntry<TextureKey, CompletableFuture<Optional<Texture>>> entry) {
            super.onScheduleEviction(entry);
            entry.getValue().complete(null);
        }
    };

    private final ConcurrentMapWithTimedEviction<TextureKey, CompletableFuture<Optional<Texture>>> textureFutures = new ConcurrentHashMapWithTimedEviction<>(texturesFutureScheduler);

    private final Cache<TextureKey, Optional<Texture>> textureCache = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    public TexturesApi(Collar collar, Supplier<ClientIdentityStore> identityStoreSupplier, Consumer<ProtocolRequest> sender) {
        super(collar, identityStoreSupplier, sender);
    }

    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        if (resp instanceof GetTextureResponse) {
            GetTextureResponse response = (GetTextureResponse) resp;
                TextureKey textureKey;
            LOGGER.info("TextureApi response: { player: " + response.player == null ? "null" : response.player.minecraftPlayer
                    + ", group: " + response.group == null ? "null" : response.group
                    + ", textureId: " + response.textureId == null ? "null" : response.textureId
                    + ", texturePath: " + response.texturePath
                    + ", type: " + response.type == null ? "null" : response.type
                    + " }"
            );
            if (response.player != null) {
                textureKey = new TextureKey(response.player.minecraftPlayer.id, response.type);
                LOGGER.info("Texture of player " + response.player + " is: " + response.textureId);
            } else if (response.group != null) {
                textureKey = new TextureKey(response.group, response.type);
                LOGGER.info("Texture of group " + response.group + " is: " + response.textureId);
            } else {
                throw new IllegalStateException("neither group or player texture was returned");
            }
            URL textureUrl = UrlBuilder.fromUrl(collar.configuration.collarServerURL).withPath(response.texturePath).toUrl();
            LOGGER.info("TextureApi textureUrl: " + textureUrl);
            Texture texture = response.texturePath == null ? null : new Texture(response.player, response.group, response.type, textureUrl);
            CompletableFuture<Optional<Texture>> removed = textureFutures.remove(textureKey);
            if (removed != null) {
                Optional<Texture> optionalTexture = Optional.ofNullable(texture);
                textureCache.put(textureKey, optionalTexture);
                removed.complete(optionalTexture);
            }
            if (texture != null) {
                collar.configuration.eventBus.dispatch(new TextureReceivedEvent(texture));
            }
            return true;
        }

        LOGGER.info("TextureApi response is not instance of  GetTextureResponse");
        return false;
    }

    /**
     * Request a player texture by future
     * @param player the texture belongs to
     * @param type the type of texture
     * @return future
     */
    public CompletableFuture<Optional<Texture>> playerTextureFuture(Player player, TextureType type) {
        CompletableFuture<Optional<Texture>> future = new CompletableFuture<>();
        textureFutures.put(new TextureKey(player.minecraftPlayer.id, type), future);
        requestPlayerTexture(player, type);
        return future;
    }

    /**
     * Request a player texture
     * @param player the texture belongs to
     * @param type the type of texture
     */
    public void requestPlayerTexture(Player player, TextureType type) {
        Optional<Texture> texture = textureCache.asMap().getOrDefault(new TextureKey(player.identity.id(), type), Optional.empty());
        if (texture.isPresent()) {
            collar.configuration.eventBus.dispatch(new TextureReceivedEvent(texture.get()));
        } else {
            sender.accept(new GetTextureRequest(player.minecraftPlayer.id, null, type));
        }
    }

    /**
     * Request a group texture by future
     * @param group the texture belongs to
     * @param type the type of texture
     * @return future
     */
    public CompletableFuture<Optional<Texture>> groupTextureFuture(Group group, TextureType type) {
        CompletableFuture<Optional<Texture>> future = new CompletableFuture<>();
        textureFutures.put(new TextureKey(group.id, type), future);
        requestGroupTexture(group, type);
        return future;
    }

    /**
     * Request a group texture
     * @param group the texture belongs to
     * @param type the type of texture
     */
    public void requestGroupTexture(Group group, TextureType type) {
        Optional<Texture> texture = textureCache.asMap().getOrDefault(new TextureKey(group.id, type), Optional.empty());
        if (texture.isPresent()) {
            collar.configuration.eventBus.dispatch(new TextureReceivedEvent(texture.get()));
        } else {
            sender.accept(new GetTextureRequest(null, group.id, type));
        }
    }

    @Override
    public void onStateChanged(Collar.State state) {}

    private static final class TextureKey {
        public final UUID id;
        public final TextureType type;

        public TextureKey(UUID id, TextureType type) {
            this.id = id;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TextureKey that = (TextureKey) o;
            return id.equals(that.id) && type == that.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, type);
        }
    }
}
