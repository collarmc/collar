package com.collarmc.server.services.profiles;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.collarmc.api.http.HttpException;
import com.collarmc.api.http.RequestContext;
import com.collarmc.api.profiles.Profile;
import com.collarmc.api.profiles.ProfileService;
import com.collarmc.api.profiles.ProfileService.GetProfileRequest;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public final class ProfileCache {
    private final ProfileService profiles;
    private final LoadingCache<UUID, Optional<Profile>> profileCache = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.SECONDS)
            .build(new CacheLoader<>() {
                @Override
                public Optional<Profile> load(UUID key) {
                    try {
                        Profile profile = profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byId(key)).profile;
                        return Optional.of(profile);
                    } catch (HttpException.NotFoundException ignored) {
                        return Optional.empty();
                    }
                }
            });

    public ProfileCache(ProfileService profiles) {
        this.profiles = profiles;
    }

    /**
     * Load the profile from cache
     * @param profile of profile
     * @return optional profile
     */
    public Optional<Profile> getById(UUID profile) {
        try {
            return profileCache.get(profile);
        } catch (ExecutionException e) {
            return Optional.empty();
        }
    }
}
