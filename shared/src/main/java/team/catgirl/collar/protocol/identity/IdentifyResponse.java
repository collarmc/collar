package team.catgirl.collar.protocol.identity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.profiles.PublicProfile;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

public final class IdentifyResponse extends ProtocolResponse {
    @JsonProperty("profile")
    public final PublicProfile profile;

    /**
     * The Collar servers emulated Mojang server id
     */
    @JsonProperty("serverPublicKey")
    public final String serverPublicKey;

    @JsonProperty("sharedSecret")
    public final byte[] sharedSecret;

    @JsonCreator
    public IdentifyResponse(@JsonProperty("identity") ServerIdentity identity,
                            @JsonProperty("profile") PublicProfile profile,
                            @JsonProperty("serverPublicKey") String serverPublicKey,
                            @JsonProperty("sharedSecret") byte[] sharedSecret) {
        super(identity);
        this.profile = profile;
        this.serverPublicKey = serverPublicKey;
        this.sharedSecret = sharedSecret;
    }
}
