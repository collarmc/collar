package com.collarmc.server.protocol;

import com.collarmc.protocol.textures.GetTextureRequest;
import com.collarmc.server.services.profiles.ProfileCache;
import com.collarmc.server.services.textures.TextureService;
import org.eclipse.jetty.websocket.api.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.collarmc.api.http.HttpException.NotFoundException;
import com.collarmc.api.http.RequestContext;
import com.collarmc.api.profiles.PublicProfile;
import com.collarmc.api.session.Player;
import com.collarmc.api.textures.TextureType;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.protocol.textures.GetTextureResponse;
import com.collarmc.security.ClientIdentity;
import com.collarmc.security.ServerIdentity;
import com.collarmc.security.mojang.MinecraftPlayer;
import com.collarmc.server.CollarServer;
import com.collarmc.server.session.SessionManager;

import java.util.function.BiConsumer;

public class TexturesProtocolHandler extends ProtocolHandler {

    private static final Logger LOGGER = LogManager.getLogger(TexturesProtocolHandler.class.getName());

    private final ServerIdentity serverIdentity;
    private final ProfileCache profiles;
    private final SessionManager sessions;
    private final TextureService textures;

    public TexturesProtocolHandler(ServerIdentity serverIdentity, ProfileCache profiles, SessionManager sessions, TextureService textures) {
        this.serverIdentity = serverIdentity;
        this.profiles = profiles;
        this.sessions = sessions;
        this.textures = textures;
    }

    @Override
    public boolean handleRequest(CollarServer collar, ProtocolRequest req, BiConsumer<ClientIdentity, ProtocolResponse> sender) {
        if (req instanceof GetTextureRequest) {
            GetTextureRequest request = (GetTextureRequest) req;
            if (request.player != null) {
                sessions.getSessionStateByPlayer(request.player).ifPresentOrElse(sessionState -> {
                    GetTextureResponse response;
                    try {
                        TextureService.Texture texture = null;
                        // If asking for a cape, lookup the default cape first
                        if (request.type == TextureType.CAPE) {
                            texture = findDefaultCape(request, sessionState);
                        }
                        // otherwise fall back to fetching any cape cape the player owns
                        if (texture == null) {
                            texture = textures.getTexture(RequestContext.ANON, new TextureService.GetTextureRequest(null, sessionState.identity.profile, request.group, request.type)).texture;
                        }
                        response = new GetTextureResponse(serverIdentity, texture.id, null, sessionState.toPlayer(), texture.url, texture.type);
                    } catch (NotFoundException ignored) {
                        LOGGER.info("Could not find texture " + request.type + " for player " + request.player);
                        response = new GetTextureResponse(serverIdentity, null, null, sessionState.toPlayer(), null, request.type);
                    }
                    sender.accept(request.identity, response);
                }, () -> {
                    sessions.findPlayer(req.identity).ifPresent(player -> {
                        Player requestedPlayer = new Player(player.identity, new MinecraftPlayer(request.player, player.minecraftPlayer.server, player.minecraftPlayer.networkId));
                        // Send this back to complete any futures on the client
                        sender.accept(req.identity, new GetTextureResponse(serverIdentity, null, null, requestedPlayer, null, request.type));
                        LOGGER.info("Could not find player " + request.player + " when fetching texture " + request.type);
                    });
                });
            } else if (request.group != null) {
                GetTextureResponse response;
                try {
                    TextureService.Texture texture = textures.getTexture(RequestContext.ANON, new TextureService.GetTextureRequest(null,null, request.group, request.type)).texture;
                    response = new GetTextureResponse(serverIdentity, texture.id, texture.group, null, texture.url, texture.type);
                } catch (NotFoundException ignored) {
                    LOGGER.info("Could not find texture " + request.type + " for group " + request.group);
                    response = new GetTextureResponse(serverIdentity, null, request.group, null, null, request.type);
                }
                sender.accept(request.identity, response);
            }
            return true;
        }
        return false;
    }

    private TextureService.Texture findDefaultCape(GetTextureRequest request, SessionManager.SessionState sessionState) {
        TextureService.Texture texture;
        PublicProfile playerProfile = profiles.getById(sessionState.identity.profile).orElseThrow(() -> new IllegalStateException("could not find profile " + sessionState.identity.profile)).toPublic();
        if (playerProfile.cape != null) {
            try {
                texture = textures.getTexture(RequestContext.ANON, new TextureService.GetTextureRequest(playerProfile.cape.texture, null, null, null)).texture;
            } catch (NotFoundException ignored) {
                LOGGER.info("Could not find texture " + playerProfile.cape.texture + " for player " + request.player);
                texture = null;
            }
        } else {
            texture = null;
        }
        return texture;
    }

    @Override
    public void onSessionStopping(ClientIdentity identity, Player player, BiConsumer<Session, ProtocolResponse> sender) {}
}
