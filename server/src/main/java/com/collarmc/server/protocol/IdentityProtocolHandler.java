package com.collarmc.server.protocol;

import com.collarmc.protocol.identity.*;
import org.eclipse.jetty.websocket.api.Session;
import com.collarmc.api.http.HttpException;
import com.collarmc.api.http.RequestContext;
import com.collarmc.api.profiles.ProfileService;
import com.collarmc.api.profiles.PublicProfile;
import com.collarmc.api.session.Player;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.security.ClientIdentity;
import com.collarmc.security.ServerIdentity;
import com.collarmc.server.CollarServer;
import com.collarmc.server.session.SessionManager;

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
                                 ProtocolRequest req,
                                 BiConsumer<ClientIdentity, ProtocolResponse> sender) {
        if (req instanceof GetIdentityRequest) {
            GetIdentityRequest request = (GetIdentityRequest) req;
            sessions.getIdentityByMinecraftPlayerId(request.player).ifPresentOrElse(identity -> {
                sender.accept(request.identity, new GetIdentityResponse(serverIdentity, request.id, identity));
            }, () -> {
                sender.accept(request.identity, new GetIdentityResponse(serverIdentity, request.id, null));
            });
            return true;
        } else if (req instanceof CreateTrustRequest) {
            CreateTrustRequest request = (CreateTrustRequest) req;
            sender.accept(request.recipient, new CreateTrustResponse(serverIdentity, request.id, request.preKeyBundle, request.identity));
            return true;
        } else if (req instanceof GetProfileRequest) {
            GetProfileRequest request = (GetProfileRequest) req;
            PublicProfile profile;
            try {
                profile = profiles.getProfile(RequestContext.SERVER, ProfileService.GetProfileRequest.byId(request.profile)).profile.toPublic();
            } catch (HttpException.NotFoundException e) {
                profile = null;
            }
            sender.accept(request.identity, new GetProfileResponse(serverIdentity, request.profile, profile));
        }
        return false;
    }

    @Override
    public void onSessionStopping(ClientIdentity identity, Player player, BiConsumer<Session, ProtocolResponse> sender) {}
}
