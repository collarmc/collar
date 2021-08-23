package com.collarmc.server.protocol;

import com.collarmc.api.http.HttpException;
import com.collarmc.api.http.RequestContext;
import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.identity.ServerIdentity;
import com.collarmc.api.profiles.ProfileService;
import com.collarmc.api.profiles.PublicProfile;
import com.collarmc.api.session.Player;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.protocol.identity.*;
import com.collarmc.server.CollarServer;
import com.collarmc.server.session.SessionManager;
import org.eclipse.jetty.websocket.api.Session;

import java.util.function.BiConsumer;

public class IdentityProtocolHandler extends ProtocolHandler {

    private final SessionManager sessions;
    private final ProfileService profiles;
    private final ServerIdentity serverIdentity;

    public IdentityProtocolHandler(SessionManager sessions,
                                   ProfileService profiles,
                                   ServerIdentity serverIdentity) {
        this.sessions = sessions;
        this.profiles = profiles;
        this.serverIdentity = serverIdentity;
    }

    @Override
    public boolean handleRequest(CollarServer collar,
                                 ClientIdentity identity, ProtocolRequest req,
                                 BiConsumer<ClientIdentity, ProtocolResponse> sender) {
        if (req instanceof GetIdentityRequest) {
            GetIdentityRequest request = (GetIdentityRequest) req;
            sessions.getIdentityByMinecraftPlayerId(request.player).ifPresentOrElse(found -> {
                sender.accept(identity, new GetIdentityResponse(request.id, found, request.player));
            }, () -> {
                sender.accept(identity, new GetIdentityResponse(request.id, null, request.player));
            });
            return true;
        } else if (req instanceof CreateTrustRequest) {
            CreateTrustRequest request = (CreateTrustRequest) req;
            sender.accept(request.recipient, new CreateTrustResponse(request.id, identity));
            return true;
        } else if (req instanceof GetProfileRequest) {
            GetProfileRequest request = (GetProfileRequest) req;
            PublicProfile profile;
            try {
                profile = profiles.getProfile(RequestContext.SERVER, ProfileService.GetProfileRequest.byId(request.profile)).profile.toPublic();
            } catch (HttpException.NotFoundException e) {
                profile = null;
            }
            sender.accept(identity, new GetProfileResponse(request.profile, profile));
        }
        return false;
    }

    @Override
    public void onSessionStopping(ClientIdentity identity, Player player, BiConsumer<Session, ProtocolResponse> sender) {}
}
