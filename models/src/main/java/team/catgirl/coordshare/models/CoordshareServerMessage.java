package team.catgirl.coordshare.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public class CoordshareServerMessage {

    @JsonProperty("identificationSuccessful")
    public final IdentificationSuccessful identificationSuccessful;
    @JsonProperty("createGroupResponse")
    public final CreateGroupResponse createGroupResponse;
    @JsonProperty("groupMembershipRequest")
    public final GroupMembershipRequest groupMembershipRequest;
    @JsonProperty("acceptGroupMembershipResponse")
    public final AcceptGroupMembershipResponse acceptGroupMembershipResponse;
    @JsonProperty("leaveGroupResponse")
    public final LeaveGroupResponse leaveGroupResponse;
    @JsonProperty("updatePositionResponse")
    public final UpdatePositionResponse updatePositionResponse;

    public CoordshareServerMessage(
            @JsonProperty("identificationSuccessful") IdentificationSuccessful identificationSuccessful,
            @JsonProperty("createGroupResponse") CreateGroupResponse createGroupResponse,
            @JsonProperty("groupMembershipRequest") GroupMembershipRequest groupMembershipRequest,
            @JsonProperty("acceptGroupMembershipResponse") AcceptGroupMembershipResponse acceptGroupMembershipResponse,
            @JsonProperty("leaveGroupResponse") LeaveGroupResponse leaveGroupResponse,
            UpdatePositionResponse updatePositionResponse) {
        this.identificationSuccessful = identificationSuccessful;
        this.createGroupResponse = createGroupResponse;
        this.groupMembershipRequest = groupMembershipRequest;
        this.acceptGroupMembershipResponse = acceptGroupMembershipResponse;
        this.leaveGroupResponse = leaveGroupResponse;
        this.updatePositionResponse = updatePositionResponse;
    }

    public static final class IdentificationSuccessful {
        public CoordshareServerMessage serverMessage() {
            return new CoordshareServerMessage(this, null, null, null, null, null);
        }
    }

    public static final class GroupMembershipRequest {
        @JsonProperty("groupId")
        public final String groupId;
        @JsonProperty("requester")
        public final UUID requester;
        @JsonProperty("UUID")
        public final UUID player;
        @JsonProperty("members")
        public final List<UUID> members;

        public GroupMembershipRequest(@JsonProperty("groupId") String groupId, @JsonProperty("requester") UUID requester, @JsonProperty("UUID") UUID player, @JsonProperty("members") List<UUID> members) {
            this.groupId = groupId;
            this.requester = requester;
            this.player = player;
            this.members = members;
        }

        public CoordshareServerMessage serverMessage() {
            return new CoordshareServerMessage(null, null, this, null, null, null);
        }
    }

    public static final class CreateGroupResponse {
        @JsonProperty("groupId")
        public final String groupId;

        public CreateGroupResponse(@JsonProperty("groupId") String groupId) {
            this.groupId = groupId;
        }

        public CoordshareServerMessage serverMessage() {
            return new CoordshareServerMessage(null, this, null, null, null, null);
        }
    }

    public static final class AcceptGroupMembershipResponse {
        @JsonProperty("groupId")
        public final String groupId;

        public AcceptGroupMembershipResponse(@JsonProperty("groupId") String groupId) {
            this.groupId = groupId;
        }

        public CoordshareServerMessage serverMessage() {
            return new CoordshareServerMessage(null, null, null, this, null, null);
        }
    }

    public static final class LeaveGroupResponse {
        @JsonProperty("groupId")
        public final String groupId;

        public LeaveGroupResponse(@JsonProperty("groupId") String groupId) {
            this.groupId = groupId;
        }

        public CoordshareServerMessage serverMessage() {
            return new CoordshareServerMessage(null, null, null, null, this, null);
        }
    }

    public static final class UpdatePositionResponse {
        @JsonProperty("groups")
        public final List<Group> groups;

        public UpdatePositionResponse(@JsonProperty("groups") List<Group> groups) {
            this.groups = groups;
        }

        public CoordshareServerMessage serverMessage() {
            return new CoordshareServerMessage(null, null, null, null, null, this);
        }
    }
}
