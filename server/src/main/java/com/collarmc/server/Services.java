package com.collarmc.server;

import com.collarmc.api.profiles.ProfileService;
import com.collarmc.security.sodium.Sodium;
import com.collarmc.server.configuration.Configuration;
import com.collarmc.server.http.AppUrlProvider;
import com.collarmc.server.security.ServerIdentityStore;
import com.collarmc.server.security.ServerIdentityStoreImpl;
import com.collarmc.server.security.hashing.PasswordHashing;
import com.collarmc.server.security.mojang.MinecraftSessionVerifier;
import com.collarmc.server.services.authentication.ServerAuthenticationService;
import com.collarmc.security.TokenCrypter;
import com.collarmc.server.services.friends.FriendsService;
import com.collarmc.server.services.groups.GroupService;
import com.collarmc.server.services.groups.GroupStore;
import com.collarmc.server.services.location.PlayerLocationService;
import com.collarmc.server.services.location.WaypointService;
import com.collarmc.server.services.profiles.ProfileCache;
import com.collarmc.server.services.profiles.ProfileServiceServer;
import com.collarmc.server.services.profiles.storage.ProfileStorage;
import com.collarmc.server.services.textures.TextureService;
import com.collarmc.server.session.ClientRegistrationService;
import com.collarmc.server.session.SessionManager;
import com.collarmc.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class Services {
    public final ObjectMapper jsonMapper;
    public final ObjectMapper packetMapper;
    public final AppUrlProvider urlProvider;
    public final ServerIdentityStore identityStore;
    public final SessionManager sessions;
    public final PasswordHashing passwordHashing;
    public final ProfileService profiles;
    public final ProfileStorage profileStorage;
    public final TokenCrypter tokenCrypter;
    public final ServerAuthenticationService auth;
    public final MinecraftSessionVerifier minecraftSessionVerifier;
    public final GroupService groups;
    public final GroupStore groupStore;
    public final PlayerLocationService playerLocations;
    public final TextureService textures;
    public final FriendsService friends;
    public final WaypointService waypoints;
    public final ClientRegistrationService deviceRegistration;
    public final ProfileCache profileCache;
    public final Sodium sodium;

    public Services(Configuration configuration) throws Exception {
        this.jsonMapper = Utils.jsonMapper();
        this.packetMapper = Utils.messagePackMapper();
        this.urlProvider = configuration.appUrlProvider;
        this.sodium = Sodium.create();
        this.identityStore = new ServerIdentityStoreImpl(configuration.database, sodium);
        this.sessions = new SessionManager(packetMapper, identityStore);
        this.deviceRegistration = new ClientRegistrationService(sessions, identityStore);
        this.passwordHashing = configuration.passwordHashing;
        this.profiles = new ProfileServiceServer(configuration.database, passwordHashing);
        this.profileCache = new ProfileCache(profiles);
        this.profileStorage = new ProfileStorage(configuration.database);
        this.tokenCrypter = configuration.tokenCrypter;
        this.auth = new ServerAuthenticationService(profiles, passwordHashing, tokenCrypter, configuration.email, urlProvider);
        this.minecraftSessionVerifier = configuration.minecraftSessionVerifier;
        this.groupStore = new GroupStore(profileCache, sessions, tokenCrypter, configuration.database);
        this.groups = new GroupService(groupStore, profileCache, sessions, identityStore.cipher());
        this.playerLocations = new PlayerLocationService(this);
        this.textures = new TextureService(configuration.database);
        this.friends = new FriendsService(configuration.database, profileCache, sessions);
        this.waypoints = new WaypointService(profileStorage);
    }
}
