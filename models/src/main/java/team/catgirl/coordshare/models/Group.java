package team.catgirl.coordshare.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class Group {

    @JsonProperty("id")
    public final String id;
    @JsonProperty("members")
    public final ImmutableMap<UUID, Member> members;

    public Group(@JsonProperty("id") String id, @JsonProperty("members") ImmutableMap<UUID, Member> members) {
        this.id = id;
        this.members = members;
    }

    public static Group newGroup(String id, Identity owner, Position ownerPosition, List<UUID> members) {
        ImmutableMap.Builder<UUID, Member> state = ImmutableMap.<UUID, Member>builder()
                .put(owner.player, new Member(owner.player, MembershipState.ACCEPTED, ownerPosition));
        members.forEach(uuid -> state.put(uuid, new Member(uuid, MembershipState.PENDING, null)));
        return new Group(id, state.build());
    }

    public Group updateMemberState(UUID uuid, MembershipState newMembershipState) {
        Member member = members.get(uuid);
        if (member == null) {
            return this;
        }
        List<Map.Entry<UUID, Member>> members = this.members.entrySet().stream().filter(entry -> !entry.getKey().equals(uuid)).collect(Collectors.toList());
        ImmutableMap.Builder<UUID, Member> state = ImmutableMap.<UUID, Member>builder().putAll(members);
        if (newMembershipState != null) {
            state = state.put(uuid, member.updateMembershipState(newMembershipState));
        }
        return new Group(id, state.build());
    }

    public Group removeMember(UUID uuid) {
        List<Map.Entry<UUID, Member>> members = this.members.entrySet().stream().filter(entry -> !entry.getKey().equals(uuid)).collect(Collectors.toList());
        ImmutableMap.Builder<UUID, Member> state = ImmutableMap.<UUID, Member>builder().putAll(members);
        return new Group(id, state.build());
    }

    public Group updateMemberPosition(UUID player, Position position) {
        Member member = this.members.get(player);
        if (member == null) {
            return this;
        }
        member = member.updatePosition(position);
        ImmutableMap.Builder<UUID, Member> state = ImmutableMap.<UUID, Member>builder().putAll(members.entrySet().stream().filter(entry -> !entry.getKey().equals(player)).collect(Collectors.toList()));
        state.put(member.player, member);
        return new Group(id, state.build());
    }

    public enum MembershipState {
        PENDING,
        ACCEPTED
    }

    public static class Member {
        @JsonProperty("player")
        public final UUID player;
        @JsonProperty("state")
        public final MembershipState membershipState;
        @JsonProperty("location")
        public final Position location;

        public Member(@JsonProperty("player") UUID player, @JsonProperty("state") MembershipState membershipState, @JsonProperty("location") Position location) {
            this.player = player;
            this.membershipState = membershipState;
            this.location = location;
        }

        public Member updateMembershipState(MembershipState membershipState) {
            return new Member(player, membershipState, location);
        }

        public Member updatePosition(Position position) {
            return new Member(player, membershipState, position);
        }
    }
}
