package com.collarmc.client.api.identity;

import com.collarmc.client.api.AbstractApi;
import com.collarmc.client.security.ClientIdentityStore;
import com.collarmc.protocol.identity.*;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.stoyanr.evictor.ConcurrentMapWithTimedEviction;
import com.stoyanr.evictor.map.ConcurrentHashMapWithTimedEviction;
import com.stoyanr.evictor.map.EvictibleEntry;
import com.stoyanr.evictor.scheduler.RegularTaskEvictionScheduler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.collarmc.api.profiles.PublicProfile;
import com.collarmc.api.session.Player;
import com.collarmc.client.Collar;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.security.ClientIdentity;
import com.collarmc.security.TokenGenerator;
import com.collarmc.security.mojang.MinecraftPlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Identity API used for resolving Collar identities from minecraft player IDs
 */
public class IdentityApi extends AbstractApi<IdentityListener> {

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
        Optional<PublicProfile> profile = profileCache.asMap().getOrDefault(player.identity.profile, Optional.empty());
        if (profile.isPresent()) {
            return CompletableFuture.completedFuture(profile);
        } else {
            CompletableFuture<Optional<PublicProfile>> future = new CompletableFuture<>();
            profileFutures.put(player.identity.profile, future);
            sender.accept(new GetProfileRequest(identity(), player.identity.profile));
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
            sender.accept(new GetIdentityRequest(identity(), id, playerId));
            return future;
        }
    }

    /**
     * Creates a bi-directional trust between the clients identity and a remote client identity
     * @param identity to create bi-directional trust with
     * @return future for when trust relationship has been created
     */
    public CompletableFuture<Optional<ClientIdentity>> createTrust(Optional<ClientIdentity> identity) {
        ClientIdentity clientIdentity = identity.orElse(null);
        if (clientIdentity == null || identityStore().isTrustedIdentity(clientIdentity)) {
            return CompletableFuture.completedFuture(identity);
        } else {
            CreateTrustRequest request = identityStore().createPreKeyRequest(clientIdentity, TokenGenerator.longToken());
            CompletableFuture<Optional<ClientIdentity>> future = new CompletableFuture<>();
            LOGGER.info("Creating trust future with " + identity + " and id " + request.id);
            identifyFutures.put(request.id, future);
            sender.accept(request);
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
        } else if (resp instanceof CreateTrustResponse) {
            CreateTrustResponse response = (CreateTrustResponse) resp;
            identityStore().trustIdentity(response.sender);
            fireListener("onIdentityTrusted", listener -> {
                listener.onIdentityTrusted(collar, this, identityStore(), response.sender);
            });
            CompletableFuture<Optional<ClientIdentity>> removed = identifyFutures.remove(response.id);
            if (removed == null) {
                LOGGER.info("Sending back a CreateTrustRequest to " + response.sender + " and id " + response.id);
                sender.accept(identityStore().createPreKeyRequest(response.sender, response.id));
            } else {
                LOGGER.info("Finished creating trust with " + response.sender + " and id " + response.id);
                removed.complete(Optional.ofNullable(response.sender));
            }
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
