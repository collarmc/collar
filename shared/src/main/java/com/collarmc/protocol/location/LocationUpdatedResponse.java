package com.collarmc.protocol.location;

import com.collarmc.api.session.Player;
import com.collarmc.protocol.ProtocolResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Sent to any client who is listening for the players location
 * For example, members of groups.
 * It is never sent to the sender
 */
public final class LocationUpdatedResponse extends ProtocolResponse {
    /** group the location is shared with */
    @JsonProperty("group")
    @Nonnull
    public final UUID group;
    /** Sender player **/
    @JsonProperty("player")
    @Nonnull
    public final Player sender;
    /** Location of player **/
    @JsonProperty("location")
    @Nullable
    public final byte[] location;

    public LocationUpdatedResponse(@Nonnull @JsonProperty("groupId") UUID group,
                                   @Nonnull @JsonProperty("player") Player sender,
                                   @Nullable @JsonProperty("location") byte[] location) {
        this.group = group;
        this.sender = sender;
        this.location = location;
    }
}
