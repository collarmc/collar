package team.catgirl.coordshare.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    public final UpdateGroupStateResponse updateGroupStateResponse;
    @JsonProperty("pong")
    public final Pong pong;

    public CoordshareServerMessage(
            @JsonProperty("identificationSuccessful") IdentificationSuccessful identificationSuccessful,
            @JsonProperty("createGroupResponse") CreateGroupResponse createGroupResponse,
            @JsonProperty("groupMembershipRequest") GroupMembershipRequest groupMembershipRequest,
            @JsonProperty("acceptGroupMembershipResponse") AcceptGroupMembershipResponse acceptGroupMembershipResponse,
            @JsonProperty("leaveGroupResponse") LeaveGroupResponse leaveGroupResponse,
            @JsonProperty("updatePositionResponse") UpdateGroupStateResponse updateGroupStateResponse,
            @JsonProperty("pong") Pong pong) {
        this.identificationSuccessful = identificationSuccessful;
        this.createGroupResponse = createGroupResponse;
        this.groupMembershipRequest = groupMembershipRequest;
        this.acceptGroupMembershipResponse = acceptGroupMembershipResponse;
        this.leaveGroupResponse = leaveGroupResponse;
        this.updateGroupStateResponse = updateGroupStateResponse;
        this.pong = pong;
    }

    public static final class Pong {
        @JsonIgnore
        public CoordshareServerMessage serverMessage() {
            return new CoordshareServerMessage(null, null, null, null, null, null, this);
        }
    }

    public static final class IdentificationSuccessful {
        @JsonIgnore
        public CoordshareServerMessage serverMessage() {
            return new CoordshareServerMessage(this, null, null, null, null, null, null);
        }
    }

    public static final class GroupMembershipRequest {
        @JsonProperty("groupId")
        public final String groupId;
        @JsonProperty("requester")
        public final UUID requester;
        @JsonProperty("members")
        public final List<UUID> members;

        public GroupMembershipRequest(@JsonProperty("groupId") String groupId, @JsonProperty("requester") UUID requester, @JsonProperty("members") List<UUID> members) {
            this.groupId = groupId;
            this.requester = requester;
            this.members = members;
        }

        @JsonIgnore
        public CoordshareServerMessage serverMessage() {
            return new CoordshareServerMessage(null, null, this, null, null, null, null);
        }
    }

    public static final class CreateGroupResponse {
        @JsonProperty("group")
        public final Group group;

        public CreateGroupResponse(@JsonProperty("group") Group group) {
            this.group = group;
        }

        @JsonIgnore
        public CoordshareServerMessage serverMessage() {
            return new CoordshareServerMessage(null, this, null, null, null, null, null);
        }
    }

    public static final class AcceptGroupMembershipResponse {
        @JsonProperty("group")
        public final Group group;

        public AcceptGroupMembershipResponse(@JsonProperty("group") Group group) {
            this.group = group;
        }

        @JsonIgnore
        public CoordshareServerMessage serverMessage() {
            return new CoordshareServerMessage(null, null, null, this, null, null, null);
        }
    }

    public static final class LeaveGroupResponse {
        @JsonProperty("groupId")
        public final String groupId;

        public LeaveGroupResponse(@JsonProperty("groupId") String groupId) {
            this.groupId = groupId;
        }

        @JsonIgnore
        public CoordshareServerMessage serverMessage() {
            return new CoordshareServerMessage(null, null, null, null, this, null, null);
        }
    }

    public static final class UpdateGroupStateResponse {
        @JsonProperty("groups")
        public final List<Group> groups;

        public UpdateGroupStateResponse(@JsonProperty("groups") List<Group> groups) {
            this.groups = groups;
        }

        @JsonIgnore
        public CoordshareServerMessage serverMessage() {
            return new CoordshareServerMessage(null, null, null, null, null, this, null);
        }
    }
}
