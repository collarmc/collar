package com.collarmc.api.groups.http;

import com.collarmc.api.groups.Group;

import java.util.List;

public class GetGroupsResponse {
    public final List<Group> groups;

    public GetGroupsResponse(List<Group> groups) {
        this.groups = groups;
    }
}
