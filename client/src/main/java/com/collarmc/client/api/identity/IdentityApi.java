package com.collarmc.client.api.identity;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.profiles.PublicProfile;
import com.collarmc.api.session.Player;
import com.collarmc.client.Collar;
import com.collarmc.client.api.AbstractApi;
import com.collarmc.client.security.ClientIdentityStore;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.protocol.identity.GetIdentityRequest;
import com.collarmc.protocol.identity.GetIdentityResponse;
import com.collarmc.protocol.identity.GetProfileRequest;
import com.collarmc.protocol.identity.GetProfileResponse;
import com.collarmc.api.security.TokenGenerator;
import com.collarmc.api.minecraft.MinecraftPlayer;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.stoyanr.evictor.ConcurrentMapWithTimedEviction;
import com.stoyanr.evictor.map.ConcurrentHashMapWithTimedEviction;
import com.stoyanr.evictor.map.EvictibleEntry;
import com.stoyanr.evictor.scheduler.RegularTaskEvictionScheduler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Identity API used for resolving Collar identities from minecraft player IDs
 */
public class IdentityApi extends AbstractApi {

    private static final Logger LOGGER = LogManager.getLogger(IdentityApi.class.getName());

    // TODO: switch this over to a global task execution scheduler when we use this pattern again
    private final RegularTaskEvictionScheduler<Long, CompletableFuture<Optional<ClientIdentity>>> identifyFuturesScheduler = new RegularTaskEvictionScheduler<Long, CompletableFuture<Optional<ClientIdentity>>>(1, TimeUnit.SECONDS) {
        /**
         * Completes the future if we do not get a response from the server
         * @param entry to evict
         */
        @Override
        protected void onScheduleEviction(EvictibleEntry<Long, CompletableFuture<Optional<ClientIdentity>>> entry) {
            super.onScheduleEviction(entry);
            entry.getValue().complete(null);
        }
    };

    private final ConcurrentMapWithTimedEviction<Long, CompletableFuture<Optional<ClientIdentity>>> identifyFutures = new ConcurrentHashMapWithTimedEviction<>(identifyFuturesScheduler);

    @SuppressWarnings("unchecked")
    private final Cache<UUID, CompletableFuture<Optional<PublicProfile>>> profileFutures = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .removalListener(removalNotification -> {
                if (removalNotification.wasEvicted()) {
                    CompletableFuture<Optional<PublicProfile>> future = (CompletableFuture<Optional<PublicProfile>>)removalNotification.getValue();
                    future.complete(Optional.empty());
                }
            }).build();

    private final Cache<UUID, Optional<PublicProfile>> profileCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build();

    private final Cache<UUID, Optional<ClientIdentity>> identityCache = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.SECONDS)
            .build();

    public IdentityApi(Collar collar, Supplier<ClientIdentityStore> identityStoreSupplier, Consumer<ProtocolRequest> sender) {
        super(collar, identityStoreSupplier, sender);
    }

    /**
     * Resolve a player by their minecraft player ID
     * @param playerId of the minecraft player
     * @return player
     */
    public CompletableFuture<Optional<Player>> resolvePlayer(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<ClientIdentity> identityOptional;
            try {
                identityOptional = identify(playerId).get(1, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                identityOptional = Optional.empty();
            }
            return identityOptional.map(identity -> new Player(identity, new MinecraftPlayer(
                            playerId,
                            collar.player().minecraftPlayer.server,
                            collar.player().minecraftPlayer.networkId)
                    )
            );
        });
    }
    /**
     * Resolve a players profile using the player object
     * @param player to find the profile of
     * @return profile
     */
    public CompletableFuture<Optional<PublicProfile>> resolveProfile(Player player) {
        Optional<PublicProfile> profile = profileCache.asMap().getOrDefault(player.identity.id(), Optional.empty());
        if (profile.isPresent()) {
            LOGGER.info("Profile resolved from cache, player identity id: " + player.identity.id());
            return CompletableFuture.completedFuture(profile);
        } else {
            CompletableFuture<Optional<PublicProfile>> future = new CompletableFuture<>();
            profileFutures.put(player.identity.id(), future);
            sender.accept(new GetProfileRequest(player.identity.id()));
            LOGGER.info("Profile resolved from request, player identity id: " + player.identity.id());
            return future;
        }
    }

    /**
     * Ask the server to identify the player ID
     * @param playerId to identify
     * @return identity future
     */
    public CompletableFuture<Optional<ClientIdentity>> identify(UUID playerId) {
        Optional<ClientIdentity> identity = identityCache.asMap().getOrDefault(playerId, Optional.empty());
        if (identity.isPresent()) {
            return CompletableFuture.completedFuture(identity);
        } else {
            CompletableFuture<Optional<ClientIdentity>> future = new CompletableFuture<>();
            long id = TokenGenerator.longToken();
            identifyFutures.put(id, future);
            sender.accept(new GetIdentityRequest(id, playerId));
            return future;
        }
    }

    @Override
    public void onStateChanged(Collar.State state) {
        if (state == Collar.State.DISCONNECTED) {
            identifyFutures.clear();
        }
    }

    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        if (resp instanceof GetIdentityResponse) {
            GetIdentityResponse response = (GetIdentityResponse) resp;
            CompletableFuture<Optional<ClientIdentity>> future = identifyFutures.get(response.id);
            Optional<ClientIdentity> identity = Optional.ofNullable(response.found);
            identityCache.put(response.player, identity);
            future.complete(identity);
            return true;
        } else if (resp instanceof GetProfileResponse) {
            GetProfileResponse response = (GetProfileResponse) resp;
            CompletableFuture<Optional<PublicProfile>> future = profileFutures.getIfPresent(response.profile.id);
            profileFutures.invalidate(response.id);
            if (future != null) {
                Optional<PublicProfile> profile = Optional.ofNullable(response.profile);
                profileCache.put(response.id, profile);
                future.complete(profile);
            }
            return true;
        }
        return false;
    }
}
