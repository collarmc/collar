package team.catgirl.collar.protocol.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.mojang.MinecraftSession;

public final class StartSessionRequest extends ProtocolRequest {
    @JsonProperty("session")
    public final MinecraftSession session;
    @JsonProperty("serverId")
    public final String serverId;

    @JsonCreator
    public StartSessionRequest(@JsonProperty("identity") ClientIdentity identity,
                               @JsonProperty("session") MinecraftSession session,
                               @JsonProperty("serverId") String serverId) {
        super(identity);
        this.session = session;
        this.serverId = serverId;
    }
}
