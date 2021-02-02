package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

import java.util.List;

public final class UpdateGroupsUpdatedResponse extends ProtocolResponse {
    @JsonProperty("groups")
    public final List<Group> groups;

    public UpdateGroupsUpdatedResponse(@JsonProperty("group") ServerIdentity identity, @JsonProperty("groups") List<Group> groups) {
        super(identity);
        this.groups = groups;
    }
}
