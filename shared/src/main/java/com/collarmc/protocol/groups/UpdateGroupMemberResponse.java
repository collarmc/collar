package com.collarmc.protocol.groups;

import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.security.ServerIdentity;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.collarmc.api.friends.Status;
import com.collarmc.api.groups.MembershipRole;
import com.collarmc.api.profiles.PublicProfile;
import com.collarmc.api.session.Player;

import java.util.UUID;

public final class UpdateGroupMemberResponse extends ProtocolResponse {
    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("player")
    public final Player player;
    @JsonProperty("profile")
    public final PublicProfile profile;
    @JsonProperty("status")
    public final Status status;
    @JsonProperty("role")
    public final MembershipRole role;

    public UpdateGroupMemberResponse(@JsonProperty("identity") ServerIdentity serverIdentity,
                                     @JsonProperty("groupId") UUID groupId,
                                     @JsonProperty("sender") Player player,
                                     @JsonProperty("profile") PublicProfile profile,
                                     @JsonProperty("status") Status status,
                                     @JsonProperty("role") MembershipRole role) {
        super(serverIdentity);
        this.groupId = groupId;
        this.player = player;
        this.profile = profile;
        this.status = status;
        this.role = role;
    }
}
