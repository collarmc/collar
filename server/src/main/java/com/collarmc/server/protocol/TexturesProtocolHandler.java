package com.collarmc.server.protocol;

import com.collarmc.api.http.HttpException.NotFoundException;
import com.collarmc.api.http.RequestContext;
import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.profiles.PublicProfile;
import com.collarmc.api.session.Player;
import com.collarmc.api.textures.TextureType;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.protocol.textures.GetTextureRequest;
import com.collarmc.protocol.textures.GetTextureResponse;
import com.collarmc.api.minecraft.MinecraftPlayer;
import com.collarmc.server.CollarServer;
import com.collarmc.server.Services;
import com.collarmc.server.services.textures.TextureService;
import com.collarmc.server.session.SessionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;

import java.util.function.BiConsumer;

public class TexturesProtocolHandler extends ProtocolHandler {

    private static final Logger LOGGER = LogManager.getLogger(TexturesProtocolHandler.class.getName());

    public TexturesProtocolHandler(Services services) {
        super(services);
    }

    @Override
    public boolean handleRequest(CollarServer collar, ClientIdentity identity, ProtocolRequest req, BiConsumer<ClientIdentity, ProtocolResponse> sender) {
        if (req instanceof GetTextureRequest) {
            GetTextureRequest request = (GetTextureRequest) req;
            if (request.player != null) {
                services.sessions.getSessionStateByPlayer(request.player).ifPresentOrElse(sessionState -> {
                    GetTextureResponse response;
                    try {
                        TextureService.Texture texture = null;
                        // If asking for a cape, lookup the default cape first
                        if (request.type == TextureType.CAPE) {
                            texture = findDefaultCape(request, sessionState);
                        }
                        // otherwise fall back to fetching any cape cape the player owns
                        if (texture == null) {
                            texture = services.textures.getTexture(RequestContext.ANON, new TextureService.GetTextureRequest(null, sessionState.identity.id(), request.group, request.type)).texture;
                        }
                        response = new GetTextureResponse(texture.id, null, sessionState.toPlayer(), texture.url, texture.type);
                    } catch (NotFoundException ignored) {
                        LOGGER.info("Could not find texture " + request.type + " for player " + request.player);
                        response = new GetTextureResponse(null, null, sessionState.toPlayer(), null, request.type);
                    }
                    sender.accept(identity, response);
                }, () -> {
                    services.sessions.findPlayer(identity).ifPresent(player -> {
                        Player requestedPlayer = new Player(player.identity, new MinecraftPlayer(request.player, player.minecraftPlayer.server, player.minecraftPlayer.networkId));
                        // Send this back to complete any futures on the client
                        sender.accept(identity, new GetTextureResponse(null, null, requestedPlayer, null, request.type));
                        LOGGER.info("Could not find player " + request.player + " when fetching texture " + request.type);
                    });
                });
            } else if (request.group != null) {
                GetTextureResponse response;
                try {
                    TextureService.Texture texture = services.textures.getTexture(RequestContext.ANON, new TextureService.GetTextureRequest(null,null, request.group, request.type)).texture;
                    response = new GetTextureResponse(texture.id, texture.group, null, texture.url, texture.type);
                } catch (NotFoundException ignored) {
                    LOGGER.info("Could not find texture " + request.type + " for group " + request.group);
                    response = new GetTextureResponse(null, request.group, null, null, request.type);
                }
                sender.accept(identity, response);
            }
            return true;
        }
        return false;
    }

    private TextureService.Texture findDefaultCape(GetTextureRequest request, SessionManager.SessionState sessionState) {
        TextureService.Texture texture;
        PublicProfile playerProfile = services.profileCache.getById(sessionState.identity.id()).orElseThrow(() -> new IllegalStateException("could not find profile " + sessionState.identity.id())).toPublic();
        if (playerProfile.cape != null) {
            try {
                texture = services.textures.getTexture(RequestContext.ANON, new TextureService.GetTextureRequest(playerProfile.cape.texture, null, null, null)).texture;
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
