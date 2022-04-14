package com.collarmc.api.groups.http;

import com.collarmc.api.groups.MembershipRole;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class AddGroupMemberRequest {
    public final UUID group;
    public final String byEmail;
    public final String playerName;
    public final MembershipRole membershipRole;

    public AddGroupMemberRequest(@JsonProperty("group") UUID group,
                                 @JsonProperty("byEmail") String byEmail,
                                 @JsonProperty("playerName") String playerName,
                                 @JsonProperty("membershipRole") MembershipRole membershipRole) {
        this.group = group;
        this.byEmail = byEmail;
        this.playerName = playerName;
        this.membershipRole = membershipRole;
    }
}
