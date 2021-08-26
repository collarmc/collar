package com.collarmc.protocol.session;

import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.protocol.SessionStopReason;
import com.collarmc.security.mojang.MinecraftSession;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Fired when the session fails in various conditions and force the client to disconnect
 */
public abstract class SessionFailedResponse extends ProtocolResponse {

    /**
     * Fired when Mojang MinecraftSession could not be validated
     */
    public static final class MojangVerificationFailedResponse extends SessionFailedResponse {
        @JsonProperty("minecraftSession")
        public final MinecraftSession minecraftSession;

        @JsonCreator
        public MojangVerificationFailedResponse(@JsonProperty("minecraftSession") MinecraftSession minecraftSession) {
            this.minecraftSession = minecraftSession;
        }
    }

    /**
     * Fired when there is a mismatch between the stored private identity token and the token supplied by the client
     * on the {@link StartSessionRequest}
     * The user will need to login to collar and delete their data before proceeding
     */
    public static final class PrivateIdentityMismatchResponse extends SessionFailedResponse {
        @JsonProperty("url")
        public final String url;

        @JsonCreator
        public PrivateIdentityMismatchResponse(@JsonProperty("url") String url) {
            this.url = url;
        }
    }

    /**
     * Fired when the server encounters an error and the client must reconnect
     */
    public static final class SessionErrorResponse extends SessionFailedResponse {
        @JsonProperty("reason")
        public final SessionStopReason reason;
        @JsonProperty("message")
        public final String message;
        @JsonCreator
        public SessionErrorResponse(@JsonProperty("reason") SessionStopReason reason,
                                    @JsonProperty("message") String message) {
            this.reason = reason;
            this.message = message;
        }
    }
}
