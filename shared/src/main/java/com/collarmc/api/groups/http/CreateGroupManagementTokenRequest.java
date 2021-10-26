package com.collarmc.api.groups.http;

import com.collarmc.api.groups.MembershipRole;

import java.util.UUID;

public class CreateGroupManagementTokenRequest {
    public final UUID group;
    public final MembershipRole role;

    public CreateGroupManagementTokenRequest(UUID group, MembershipRole role) {
        this.group = group;
        this.role = role;
    }
}
