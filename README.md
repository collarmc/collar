# collar

Online services for minecraft mods.

Including:
* Coordinate sharing
* Waypoints
* Friend lists

## To-do's

### Short term
* End to end encryption (or at least session check?) to verify that sender is who they say they are
* Tests for `GroupManager` as it is somewhat complicated
* Group waypoints
* Friend lists

### Long term
* End-to-end functional tests (before switching to redis for state)
* Scale from 100s to 1000s of players by using Redis to handle sessions and group state.

## Building
To build run you will need to have Maven 3 installed. 

Execute:
`mvn clean install`

## Using in Forge

We need to shadow the client dependency in your Jar and relocate the `team.catgirl.coordshare` package
in order to avoid conflicts with other mods.

```
repositories {
  mavenLocal()
}
dependencies {
  compile group: 'team.catgirl.coordshare', name: 'client', version: '1.0-SNAPSHOT'
}
apply plugin: 'com.github.johnrengelman.shadow'
shadowJar {
  // Only shadow fluent-hc
  dependencies {
    include(dependency('team.catgirl.coordshare:client:.*'))
  }

  // Replace com.yourpackage with your mods package
  relocate 'team.catgirl.coordshare', 'com.yourpackage.team.catgirl.coordshare'

  classifier '' // Replace the default JAR
}
reobf {
  shadowJar {} // Reobfuscate the shadowed JAR
}
```

## ForgeHax example

### Coordinate sharing
You can easily skid this as a ForgeHax module like so:

```
package com.matt.forgehax.mods;


import com.matt.forgehax.asm.events.LocalPlayerUpdateMovementEvent;
import com.matt.forgehax.events.Render2DEvent;
import com.matt.forgehax.mods.services.FriendService;
import com.matt.forgehax.util.SimpleTimer;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.math.AngleHelper;
import com.matt.forgehax.util.math.Plane;
import com.matt.forgehax.util.math.VectorUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.apache.logging.log4j.Level;
import org.lwjgl.opengl.GL11;
import team.catgirl.coordshare.client.CoordshareClient;
import team.catgirl.coordshare.client.CoordshareListener;
import team.catgirl.coordshare.models.CoordshareServerMessage;
import team.catgirl.coordshare.models.CoordshareServerMessage.CreateGroupResponse;
import team.catgirl.coordshare.models.CoordshareServerMessage.GroupMembershipRequest;
import team.catgirl.coordshare.models.CoordshareServerMessage.LeaveGroupResponse;
import team.catgirl.coordshare.models.CoordshareServerMessage.UpdatePlayerStateResponse;
import team.catgirl.coordshare.models.Group;
import team.catgirl.coordshare.models.Identity;
import team.catgirl.coordshare.models.Position;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.matt.forgehax.Helper.getModManager;
import static com.matt.forgehax.Helper.printInform;

/**
 *  orsond:
 *  .Coordshare group create [user1] [user2]
 *  pepsi
 *  *Orsond wants to share coords with you in group [id]*
 *  .Coordshare group accept [id]
 *  .Coordshare group leave [id]
 *  .Coordshare group add [user]
 */
@RegisterMod
public class CoordShareMod extends ToggleMod {

  private CoordshareClient client;
  private LinkedList<Group> groups = new LinkedList<>(); // TODO: replace with a proper state machine
  private final LinkedList<GroupMembershipRequest> invites = new LinkedList<>(); // TODO: replace with a proper state machine
  private final SimpleTimer positionUpdateTimer = new SimpleTimer();
  private boolean isConnectedToMCServer;

  public CoordShareMod() {
    super(Category.RENDER, "CoordShare", false, "Share coordinates in real time with your friends.");
    client = new CoordshareClient(server.get());
  }

  public final Setting<String> server =
      getCommandStub()
          .builders()
          .<String>newSettingBuilder()
          .name("server")
          .description("Coordshare server url")
          .defaultTo("http://coordshare.herokuapp.com")
          .build();

  @Override
  protected void onLoad() {
    super.onLoad();
    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("create")
        .description("create a group")
        .processor(
            data -> {
              data.requiredArguments(1);
              List<String> users = new ArrayList<>();
              for (int i = 0; i < data.getArgumentCount(); i++) {
                users.add(data.getArgumentAsString(i));
              }
              createGroup(users);
            })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("leave")
        .description("leave a group")
        .processor(
            data -> {
              data.requiredArguments(1);
              String argumentAsString = data.getArgumentAsString(0);
              int position;
              try {
                position = Integer.parseInt(argumentAsString);;
              } catch (NumberFormatException e) {
                printInform("Invalid argument " + argumentAsString);
                return;
              }
              if (position == -1) {
                printInform("Invalid group " + argumentAsString);
                return;
              }
              Group group = groups.get(position);
              if (group == null) {
                printInform("Invalid group " + argumentAsString);
                return;
              }
              leaveGroup(group);
            })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("list")
        .description("list groups")
        .processor(
            data -> {
              if (groups.size() == 0) {
                printInform("You are not a member of any groups");
              } else {
                printInform("You are a member of the following groups:");
                for (int i = 0; i < groups.size(); i++) {
                  Group group = groups.get(i);
                  Group.Member owner = group.members.values().stream().filter(member -> member.membershipRole == Group.MembershipRole.OWNER).findFirst().orElse(null);
                  List<UUID> memberIds = group.members.values().stream().map(candidate -> candidate.player).collect(Collectors.toList());
                  if (owner == null) {
                    List<String> playerNames = getPlayerNames(memberIds).collect(Collectors.toList());
                    if (playerNames.isEmpty()) {
                      printInform("[%s] group all by yourself", i);
                    } else {
                      printInform("[%s] group with %s", i, playerNames);
                    }
                  } else {
                    List<String> ids = getPlayerNamesWithout(memberIds, owner.player).collect(Collectors.toList());
                    if (ids.isEmpty()) {
                      printInform("[%s] group started by %s", i, getNameFromPlayerId(owner.player));
                    } else {
                      printInform("[%s] group started by %s with %s", i, getNameFromPlayerId(owner.player), playersIdsToStringWithout(memberIds, owner.player));
                    }
                  }
                }
              }
            })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("invite")
        .description("Invite to group")
        .processor(
            data -> {
              if (groups.isEmpty()) {
                printInform("You have no groups");
              } else {
                data.requiredArguments(2);
                String argumentAsString = data.getArgumentAsString(0);
                int position;
                try {
                  position = Integer.parseInt(argumentAsString);;
                } catch (NumberFormatException e) {
                  printInform("Invalid argument " + argumentAsString);
                  return;
                }
                if (position == -1) {
                  printInform("Invalid group " + argumentAsString);
                  return;
                }
                Group group = groups.get(position);
                if (group == null) {
                  printInform("Invalid group " + argumentAsString);
                  return;
                }
                Group.Member me = group.members.get(MC.player.getGameProfile().getId());
                if (me.membershipRole != Group.MembershipRole.OWNER) {
                  printInform("You did not start this group");
                  return;
                }
                List<String> playerNames = new ArrayList<>();
                for (int i = 1; i < data.getArgumentCount(); i++) {
                  playerNames.add(data.getArgumentAsString(i));
                }
                invite(group, playerNames);
              }
            })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("accept")
        .description("accept invite")
        .processor(
            data -> {
              if (data.getArgumentCount() == 0) {
                if (invites.isEmpty()) {
                  printInform("You have no group invitations");
                } else {
                  data.requiredArguments(1);
                  String argumentAsString = data.getArgumentAsString(0);
                  int position;
                  try {
                    position = Integer.parseInt(argumentAsString);;
                  } catch (NumberFormatException e) {
                    printInform("Invalid argument " + argumentAsString);
                    return;
                  }
                  if (position == -1) {
                    printInform("Invalid invite " + argumentAsString);
                    return;
                  }
                  GroupMembershipRequest invite = invites.get(position);
                  if (invite == null) {
                    printInform("Invalid invite " + argumentAsString);
                    return;
                  }
                  if (invite.members.isEmpty()) {
                    printInform("Joined group started by %s", getNameFromPlayerId(invite.requester));
                  } else {
                    printInform("Joined group started by %s with %s", getNameFromPlayerId(invite.requester), playersIdsToStringWithout(invite.members, invite.requester));
                  }
                }
              } else {
                String argumentAsString = data.getArgumentAsString(0);
                acceptInvite(Integer.valueOf(argumentAsString));
              }
            })
        .build();
  }

  @Override
  protected void onEnabled() {
    super.onEnabled();
    if (isEnabled()) {
      startSharing();
    }
  }

  @Override
  protected void onDisabled() {
    super.onDisabled();
    stopSharing();
  }

  ///
  /// Event subscriptions
  ///

  @SubscribeEvent
  public void onDrawScreen(Render2DEvent event) {
    if (MC.world == null) {
      return;
    }
    drawLines(event);
  }

  @SubscribeEvent
  public void onEntityJoinWorld(EntityJoinWorldEvent event) {
    if (event.getEntity() == null || !(event.getEntity() instanceof EntityPlayer) || !event.getWorld().isRemote) {
      return;
    }
    GameProfile gameProfile = ((EntityPlayer) event.getEntity()).getGameProfile();
    if (!gameProfile.getId().equals(MC.player.getGameProfile().getId())) {
      return;
    }
    if (!isEnabled()) {
      return;
    }
    LOGGER.info("Connecting with coordshare server at " + server.get());
    startSharing();
  }

  @SubscribeEvent
  public void ServerDisconnectionFromClientEvent(PlayerEvent.PlayerLoggedOutEvent event) {
    LOGGER.info("Disconnected from coordshare server");
    stopSharing();
  }

  @SubscribeEvent
  public void playerUpdate(LocalPlayerUpdateMovementEvent event) {
    if ((client.isConnected() && groups.size() > 0) && (positionUpdateTimer.isStopped() || positionUpdateTimer.hasTimeElapsed(TimeUnit.SECONDS.toMillis(3)))) {
      if (!MC.player.getGameProfile().getId().equals(MC.player.getGameProfile().getId())) {
        return;
      }
      Position pos = playerPosition(event.getLocalPlayer());
      try {
        LOGGER.info("Sending player position " + pos.toString());
        client.updatePosition(pos);
      } catch (IOException e) {
        LOGGER.error("Could not send location", e);
      }
      positionUpdateTimer.start();
    }
  }

  ///
  /// Client implementation
  ///

  public void startSharing() {
    LOGGER.info("Coordshare starting");
    if (client == null) {
      client = new CoordshareClient(server.get());
    }
    Identity identity = Identity.from(MC.getSession().getSessionID(), EntityPlayer.getUUID(MC.player.getGameProfile()));
    client.connect(identity, new CoordshareListenerImpl());
    positionUpdateTimer.start();
  }

  private void stopSharing() {
    LOGGER.info("Coordshare stopping");
    if (client != null && client.isConnected()) {
      client.disconnect();
    }
    client = null;
    positionUpdateTimer.stop();
  }

  private void acceptInvite(Integer inviteIndex) {
    GroupMembershipRequest groupMembershipRequest = invites.get(inviteIndex);
    if (groupMembershipRequest != null) {
      try {
        client.acceptGroupRequest(groupMembershipRequest.groupId, Group.MembershipState.ACCEPTED);
      } catch (IOException e) {
        LOGGER.error("Could not create group", e);
      }
    }
  }

  private void createGroup(List<String> players) {
    if (!client.isConnected()) {
      LOGGER.info("coords client not connected");
      return;
    }
    NetHandlerPlayClient connection = MC.getConnection();
    if (connection == null) {
      LOGGER.info("Connection is null - single player?");
      return;
    }
    List<UUID> collect = getPlayers(players).map(GameProfile::getId).collect(Collectors.toList());
    if (collect.isEmpty()) {
      LOGGER.info("No players could be found");
      return;
    }
    try {
      Position from = playerPosition(MC.player);
      client.createGroup(collect, from);
    } catch (IOException e) {
      LOGGER.error("Could not create group", e);
    }
  }

  private void leaveGroup(Group group) {
    try {
      client.leaveGroup(group);
    } catch (IOException e) {
      LOGGER.error("Could not leave group", e);
    }
  }

  private void invite(Group group, List<String> playerNames) {
    List<UUID> playerIds = getPlayers(playerNames).map(GameProfile::getId).collect(Collectors.toList());
    try {
      client.invite(group, playerIds);
    } catch (IOException e) {
      LOGGER.error("Could not send invites group", e);
    }
  }

  ///
  /// Utilities
  ///

  private static Stream<GameProfile> getPlayers(List<String> playerNames) {
    NetHandlerPlayClient connection = MC.getConnection();
    if (connection == null) {
      return Stream.empty();
    }
    return connection.getPlayerInfoMap().stream()
        .map(NetworkPlayerInfo::getGameProfile)
        .filter(gameProfile -> playerNames.contains(gameProfile.getName()));
  }

  private static String playersIdsToString(List<UUID> players) {
    return playersIdsToStringWithout(players, null);
  }

  private static String playersIdsToStringWithout(List<UUID> players, UUID uuid) {
    StringJoiner joiner = new StringJoiner(",");
    getPlayerNamesWithout(players, uuid).filter(Objects::isNull).forEach(joiner::add);
    return joiner.toString();
  }

  private static Stream<String> getPlayerNames(List<UUID> players) {
    return getGameProfiles(players).map(GameProfile::getName);
  }

  private static Stream<String> getPlayerNamesWithout(List<UUID> players, UUID uuid) {
    return getGameProfiles(players).filter(gameProfile -> !gameProfile.getId().equals(uuid)).map(GameProfile::getName);
  }

  private static Stream<GameProfile> getGameProfiles(List<UUID> players) {
    return players.stream().map(CoordShareMod::getPlayerInfo).filter(Objects::isNull);
  }

  private static String getNameFromPlayerId(UUID player) {
    GameProfile playerInfo = getPlayerInfo(player);
    if (playerInfo == null) {
      return null;
    }
    return playerInfo.getName();
  }

  private static GameProfile getPlayerInfo(UUID player) {
    NetHandlerPlayClient connection = MC.getConnection();
    if (connection == null) {
      return null;
    }
    NetworkPlayerInfo playerInfo = connection.getPlayerInfo(player);
    if (playerInfo == null) {
      return null;
    }
    return playerInfo.getGameProfile();
  }

  private static Position playerPosition(EntityPlayer player) {
    return new Position(player.posX, player.posY, player.posZ, player.dimension);
  }

  ///
  /// Drawing code
  ///

  private void drawLines(Render2DEvent event) {
    // TODO: this is copied from Tracers as a proof of concept
    GlStateManager.enableBlend();
    GlStateManager.tryBlendFuncSeparate(
        GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
        GlStateManager.SourceFactor.ONE,
        GlStateManager.DestFactor.ZERO);
    GlStateManager.disableTexture2D();

    GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
    GL11.glEnable(GL11.GL_LINE_SMOOTH);

    groups.stream()
        .flatMap(group -> group.members.values().stream())
        .filter(member -> member.location != null)
        .filter(member -> !MC.player.getGameProfile().getId().equals(member.player) && member.location.dimension == MC.player.dimension)
        .collect(Collectors.toMap(member -> member.player, member -> member, (member, member2) -> member))
        .forEach((s, member) -> drawLine(s, member.location, event));

    GlStateManager.enableTexture2D();
    GlStateManager.disableBlend();
    GlStateManager.glLineWidth(1.0f);
    GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
    GL11.glDisable(GL11.GL_LINE_SMOOTH);

    GlStateManager.color(1.f, 1.f, 1.f, 1.f);
  }

  private void drawLine(UUID player, Position position, Render2DEvent event) {
    final Tracers.Mode dm = Tracers.Mode.BOTH;
    final double cx = event.getScreenWidth() / 2.f;
    final double cy = event.getScreenHeight() / 2.f;
    final float width = 1.5f;

    Entity entity = MC.world.loadedEntityList.stream()
        .filter(candidate -> candidate instanceof EntityPlayer && ((EntityPlayer) candidate)
            .getGameProfile().getId().equals(player)).findFirst()
        .orElse(null);

    Vec3d entityPos;
    if (entity == null) {
      entityPos = new Vec3d(position.x, position.y, position.z);
    } else {
      entityPos = entity.getPositionVector();
    }

    Plane screenPos = VectorUtils.toScreen(entityPos);
    Color color;
    if(getModManager().get(FriendService.class).get().isFriendly(player.toString())) {
      color = Colors.AQUA;
    } else {
      double entityPosX = entityPos.x;
      double entityPosY = entityPos.y;
      double entityPosZ = entityPos.z;
      color = Colors.RED;
    }

    GlStateManager.color(
        color.getRedAsFloat(),
        color.getGreenAsFloat(),
        color.getBlueAsFloat(),
        color.getAlphaAsFloat());

    GlStateManager.translate(0, 0, 15.f);

    if (dm.equals(Tracers.Mode.BOTH) || dm.equals(Tracers.Mode.ARROWS)) {
      if (!screenPos.isVisible()) {
        // get position on ellipse

        // dimensions of the ellipse
        final double dx = cx - 2;
        final double dy = cy - 20;

        // ellipse = x^2/a^2 + y^2/b^2 = 1
        // e = (pos - C) / d
        //  C = center vector
        //  d = dimensions
        double ex = (screenPos.getX() - cx) / dx;
        double ey = (screenPos.getY() - cy) / dy;

        // normalize
        // n = u/|u|
        double m = Math.abs(Math.sqrt(ex * ex + ey * ey));
        double nx = ex / m;
        double ny = ey / m;

        // scale
        // p = C + dot(n,d)
        double x = cx + nx * dx;
        double y = cy + ny * dy;

        // --------------------
        // now rotate triangle

        // point - center
        // w = <px - cx, py - cy>
        double wx = x - cx;
        double wy = y - cy;

        // u = <w, 0>
        double ux = event.getScreenWidth();
        double uy = 0.D;

        // |u|
        double mu = Math.sqrt(ux * ux + uy * uy);
        // |w|
        double mw = Math.sqrt(wx * wx + wy * wy);

        // theta = dot(u,w)/(|u|*|w|)
        double ang = Math.toDegrees(Math.acos((ux * wx + uy * wy) / (mu * mw)));

        // don't allow NaN angles
        if (ang == Float.NaN) {
          ang = 0;
        }

        // invert
        if (y < cy) {
          ang *= -1;
        }

        // normalize
        ang = (float) AngleHelper.normalizeInDegrees(ang);

        // --------------------

        int size = 8;

        GlStateManager.pushMatrix();

        GlStateManager.translate(x, y, 0);
        GlStateManager.rotate((float) ang, 0.f, 0.f, size / 2.f);

        GlStateManager.color(
            color.getRedAsFloat(),
            color.getGreenAsFloat(),
            color.getBlueAsFloat(),
            color.getAlphaAsFloat());

        GlStateManager.glBegin(GL11.GL_TRIANGLES);
        {
          GL11.glVertex2d(0, 0);
          GL11.glVertex2d(-size, -size);
          GL11.glVertex2d(-size, size);
        }
        GlStateManager.glEnd();

        GlStateManager.popMatrix();
      }
    }

    if (dm.equals(Tracers.Mode.BOTH) || dm.equals(Tracers.Mode.LINES)) {
      GlStateManager.glLineWidth(width);
      GlStateManager.glBegin(GL11.GL_LINES);
      {
        GL11.glVertex2d(cx, cy);
        GL11.glVertex2d(screenPos.getX(), screenPos.getY());
      }
      GlStateManager.glEnd();
    }

    GlStateManager.translate(0, 0, -15.f);
  }

  class CoordshareListenerImpl implements CoordshareListener {
    @Override
    public void onGroupCreated(CoordshareClient client, CreateGroupResponse resp) {
      groups.add(resp.group);
      printInform("Created group with %s", playersIdsToString(resp.group.members.keySet().asList()));
    }

    @Override
    public void onGroupJoined(CoordshareClient client, CoordshareServerMessage.AcceptGroupMembershipResponse acceptGroupMembershipResponse) {
      Group newGroup = acceptGroupMembershipResponse.group;
      groups.add(newGroup);
      printInform("Joined group with %s", playersIdsToString(newGroup.members.keySet().asList()));
    }

    @Override
    public void onGroupLeft(CoordshareClient client, LeaveGroupResponse resp) {
      groups.stream().filter(group -> group.id.equals(resp.groupId)).findFirst().ifPresent(group -> {
        groups.remove(group);
        printInform("Left group with %s", resp.groupId, playersIdsToString(group.members.keySet().asList()));
      });
    }

    @Override
    public void onGroupMembershipRequested(CoordshareClient client, GroupMembershipRequest resp) {
      invites.add(resp);
      LOGGER.log(Level.INFO, "Membership requested by " + resp.requester + " for group " + resp.groupId);
      invites.stream().filter(invite -> invite.groupId.equals(resp.groupId)).findFirst().ifPresent(group -> {
        if (resp.members.isEmpty()) {
          printInform("Received group invite from %s to share coordinates", getNameFromPlayerId(resp.requester));
        } else {
          printInform("Received group invite from %s to share coordinates with %s", getNameFromPlayerId(resp.requester), playersIdsToString(resp.members));
        }
        printInform("Type '.Coordshare acceptInvite' to list invites");
      });
    }

    @Override
    public void onGroupUpdated(CoordshareClient client, UpdatePlayerStateResponse resp) {
      groups = new LinkedList<>(resp.groups);
      LOGGER.log(Level.INFO, "Received group updates");
    }

    @Override
    public void onSessionCreated(CoordshareClient client) {
      printInform("Connected to Coordshare server %s", server.get());
    }

    @Override
    public void onDisconnect(CoordshareClient client) {
      printInform("Disconnected from Coordshare server %s", server.get());
      stopSharing();
      // TODO: gracefully handle reconnects
    }

    @Override
    public void onPongReceived(CoordshareServerMessage.Pong pong) {
      LOGGER.log(Level.INFO, "Pong received");
    }
  }
}
```