package com.collarmc.server.services.groups;

import com.collarmc.api.groups.*;
import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.server.configuration.Configuration;
import com.collarmc.server.junit.MongoDatabaseTestRule;
import com.collarmc.server.services.profiles.ProfileCache;
import com.collarmc.server.services.profiles.ProfileServiceServer;
import com.collarmc.server.session.SessionManager;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import com.collarmc.api.http.RequestContext;
import com.collarmc.api.profiles.Profile;
import com.collarmc.api.profiles.ProfileService;
import com.collarmc.api.profiles.ProfileService.CreateProfileRequest;
import com.collarmc.api.session.Player;
import com.collarmc.security.mojang.MinecraftPlayer;
import com.collarmc.utils.Utils;

import java.util.List;
import java.util.UUID;

public class GroupStoreTest {
    @Rule
    public MongoDatabaseTestRule dbRule = new MongoDatabaseTestRule();

    @Test
    public void crud() {
        ProfileService profiles = new ProfileServiceServer(dbRule.db, Configuration.defaultConfiguration().passwordHashing);
        ProfileCache profileCache = new ProfileCache(profiles);
        Profile ownerProfile = profiles.createProfile(RequestContext.ANON, new CreateProfileRequest("owner@example.com", "cute", "owner")).profile;
        GroupStore store = new GroupStore(profileCache, new SessionManager(Utils.messagePackMapper(), null), dbRule.db);

        UUID groupId = UUID.randomUUID();
        Player owner = new Player(new ClientIdentity(ownerProfile.id, null, null), new MinecraftPlayer(UUID.randomUUID(), "2b2t.org", 1));

        store.upsert(Group.newGroup(groupId, "The Spawnmasons", GroupType.GROUP, new MemberSource(owner, null), List.of()));

        Group group = store.findGroup(groupId).orElseThrow(() -> new IllegalStateException("cant find group"));
        Assert.assertEquals(groupId, group.id);
        Assert.assertEquals("The Spawnmasons", group.name);
        Assert.assertEquals(GroupType.GROUP, group.type);
        Assert.assertEquals(1, group.members.size());
        Assert.assertTrue(group.members.stream().anyMatch(member -> member.player.equals(owner)));

        Profile player1Profile = profiles.createProfile(RequestContext.ANON, new CreateProfileRequest("player1@example.com", "cute", "player1")).profile;
        Player player1 = new Player(new ClientIdentity(player1Profile.id, null, null), null);
        Profile player2Profile = profiles.createProfile(RequestContext.ANON, new CreateProfileRequest("player2@example.com", "cute", "player2")).profile;
        Player player2 = new Player(new ClientIdentity(player2Profile.id, null, null), null);

        group = store.addMembers(groupId, List.of(new MemberSource(player1, null), new MemberSource(player2, null)), MembershipRole.MEMBER, MembershipState.ACCEPTED).orElse(null);
        Assert.assertNotNull(group);

        Member member1 = group.members.stream().filter(member -> member.player.equals(player1)).findFirst().orElseThrow();
        Assert.assertEquals(player1.identity.profile, member1.player.identity.profile);
        Assert.assertEquals(MembershipRole.MEMBER, member1.membershipRole);
        Assert.assertEquals(MembershipState.ACCEPTED, member1.membershipState);

        Member member2 = group.members.stream().filter(member -> member.player.equals(player2)).findFirst().orElseThrow();
        Assert.assertEquals(player2.identity.profile, member2.player.identity.profile);
        Assert.assertEquals(MembershipRole.MEMBER, member2.membershipRole);
        Assert.assertEquals(MembershipState.ACCEPTED, member2.membershipState);

        Assert.assertEquals(groupId, store.findGroupsContaining(player1).findFirst().map(group1 -> group1.id).orElse(null));

        group = store.updateMember(groupId, player1.identity.profile, MembershipRole.MEMBER, MembershipState.DECLINED).orElse(null);
        Assert.assertNotNull(group);
        member1 = group.members.stream().filter(member -> member.player.equals(player1)).findFirst().orElseThrow();
        Assert.assertEquals(player1.identity.profile, member1.player.identity.profile);
        Assert.assertEquals(MembershipRole.MEMBER, member1.membershipRole);
        Assert.assertEquals(MembershipState.DECLINED, member1.membershipState);
        Assert.assertEquals(3, group.members.size());

        group = store.removeMember(groupId, player1.identity.profile).orElse(null);
        Assert.assertNotNull(group);
        Assert.assertEquals(2, group.members.size());
        Assert.assertFalse(group.members.stream().anyMatch(member -> member.player.equals(player1)));
        Assert.assertTrue(group.members.stream().anyMatch(member -> member.player.equals(player2)));
        Assert.assertTrue(group.members.stream().anyMatch(member -> member.player.equals(owner)));

        store.delete(groupId);
        Assert.assertFalse(store.findGroup(groupId).isPresent());
    }
}
