package com.collarmc.server.services.location;

import com.collarmc.api.groups.MemberSource;
import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.session.Player;
import com.collarmc.api.minecraft.MinecraftPlayer;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;
import java.util.UUID;

public class NearbyGroupsTest {
    @Test
    public void calculateGroup() {
        Player alice = new Player(new ClientIdentity(UUID.randomUUID(), null), new MinecraftPlayer(UUID.randomUUID(), "cute", 1));
        Player bob = new Player(new ClientIdentity(UUID.randomUUID(), null), new MinecraftPlayer(UUID.randomUUID(), "cute", 1));
        Player eve = new Player(new ClientIdentity(UUID.randomUUID(), null), new MinecraftPlayer(UUID.randomUUID(), "cute", 1));

        NearbyGroups groups = new NearbyGroups();
        NearbyGroups.Result result = groups.updateNearbyGroups(new MemberSource(alice, null), Set.of("alice", "bob"));
        Assert.assertTrue(result.add.isEmpty());
        Assert.assertTrue(result.remove.isEmpty());

        result = groups.updateNearbyGroups(new MemberSource(bob, null), Set.of("alice", "bob"));
        Assert.assertFalse(result.add.isEmpty());
        Assert.assertTrue(result.remove.isEmpty());

        NearbyGroup group = result.add.values().iterator().next();
        Assert.assertTrue(group.players.contains(new MemberSource(alice, null)));
        Assert.assertTrue(group.players.contains(new MemberSource(bob, null)));

        result = groups.updateNearbyGroups(new MemberSource(eve, null), Set.of("eve", "alice"));
        Assert.assertTrue(result.add.isEmpty());
        Assert.assertTrue(result.remove.isEmpty());

        result = groups.updateNearbyGroups(new MemberSource(bob, null), Set.of("alice", "bob"));
        Assert.assertTrue(result.add.isEmpty());
        Assert.assertTrue(result.remove.isEmpty());

        result = groups.updateNearbyGroups(new MemberSource(alice, null), Set.of("alice", "eve"));
        Assert.assertFalse(result.add.isEmpty());

        group = result.add.values().iterator().next();
        Assert.assertEquals(2, group.players.size());
        Assert.assertTrue(group.players.contains(new MemberSource(alice, null)));
        Assert.assertTrue(group.players.contains(new MemberSource(eve, null)));

        Assert.assertFalse(result.remove.isEmpty());
        group = result.remove.values().iterator().next();
        Assert.assertEquals(2, group.players.size());
        Assert.assertTrue(group.players.contains(new MemberSource(alice, null)));
        Assert.assertTrue(group.players.contains(new MemberSource(bob, null)));
    }
}
