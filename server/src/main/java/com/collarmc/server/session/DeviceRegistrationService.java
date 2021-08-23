package com.collarmc.server.session;

import com.collarmc.api.http.HttpException;
import com.collarmc.api.http.HttpException.ServerErrorException;
import com.collarmc.api.profiles.PublicProfile;
import com.collarmc.protocol.devices.DeviceRegisteredResponse;
import com.collarmc.security.TokenGenerator;
import com.collarmc.security.messages.CipherException;
import com.collarmc.server.security.ServerIdentityStore;
import com.collarmc.server.services.devices.DeviceService.CreateDeviceResponse;
import org.eclipse.jetty.websocket.api.Session;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Keeps track of device registrations
 */
public final class DeviceRegistrationService {
    private final ConcurrentMap<String, Session> devicesWaitingToRegister = new ConcurrentHashMap<>();
    private final ConcurrentMap<Session, String> sessionToRegistrationToken = new ConcurrentHashMap<>();
    private final SessionManager sessions;
    private final ServerIdentityStore identityStore;

    public DeviceRegistrationService(SessionManager sessions, ServerIdentityStore identityStore) {
        this.sessions = sessions;
        this.identityStore = identityStore;
    }

    /**
     * Creates a new registration token for the given session
     * @param session to issue a token for
     * @return token
     */
    public String createDeviceRegistrationToken(@Nonnull Session session) {
        String token = TokenGenerator.urlToken();
        devicesWaitingToRegister.put(token, session);
        sessionToRegistrationToken.put(session, token);
        return token;
    }

    /**
     * Called when the device has finished registering
     * @param profile owner of the device
     * @param token token being claimed
     * @param resp of the device being created
     */
    public void onDeviceRegistered(@Nonnull PublicProfile profile,
                                   @Nonnull String token,
                                   @Nonnull CreateDeviceResponse resp) {
        Session session = devicesWaitingToRegister.get(token);
        if (session == null) {
            throw new HttpException.NotFoundException("session does not exist");
        }
        try {
            sessions.send(session, null, new DeviceRegisteredResponse(identityStore.identity(), profile, resp.device.deviceId));
        } catch (IOException | CipherException e) {
            throw new ServerErrorException("could not send DeviceRegisteredResponse", e);
        }
    }

    /**
     * Invalidates any registrations open belonging to the session when session is closed
     * @param session closing
     */
    public void onSessionClosed(Session session) {
        String token = sessionToRegistrationToken.remove(session);
        if (token != null) {
            devicesWaitingToRegister.remove(token);
        }
    }
}
