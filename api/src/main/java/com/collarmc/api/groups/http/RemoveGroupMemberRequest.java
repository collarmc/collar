package com.collarmc.api.groups.http;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class RemoveGroupMemberRequest {
    @JsonProperty("group")
    public final UUID group;
    @JsonProperty("byEmail")
    public final String byEmail;
    @JsonProperty("byUsername")
    public final String byUsername;
    @JsonProperty("byPlayerName")
    public final String byPlayerName;

    public RemoveGroupMemberRequest(@JsonProperty("group") UUID group,
                                    @JsonProperty("byEmail") String byEmail,
                                    @JsonProperty("byUsername") String byUsername,
                                    @JsonProperty("byPlayerName") String byPlayerName) {
        this.group = group;
        this.byEmail = byEmail;
        this.byUsername = byUsername;
        this.byPlayerName = byPlayerName;
    }
}
