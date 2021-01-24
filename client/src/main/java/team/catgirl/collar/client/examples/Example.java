package team.catgirl.collar.client.examples;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import team.catgirl.collar.client.CollarClient;
import team.catgirl.collar.client.CollarListener;
import team.catgirl.collar.messages.ServerMessage;
import team.catgirl.collar.messages.ServerMessage.GroupMembershipRequest;
import team.catgirl.collar.models.Group;
import team.catgirl.collar.models.Identity;
import team.catgirl.collar.models.Position;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Example {
    public static void main(String[] args) throws Exception {
        String baseUrl = "http://localhost:3000";

        CollarClient client1 = new CollarClient(baseUrl);
        UUID player1 = UUID.randomUUID();
        System.out.println("Player1 " + player1);
        client1.connect(Identity.from("client1", player1), new Listener1(player1));

        CollarClient client2 = new CollarClient(baseUrl);
        UUID player2 = UUID.randomUUID();
        System.out.println("Player2 " + player2);
        client2.connect(Identity.from("client2", player2), new Listener2(player2, player1));

        // Spin
        while (client1.isConnected() && client2.isConnected()) {
            Thread.sleep(1000);
        }
    }

    public static class Listener1 extends AbstractCollarListener {

        public Listener1(UUID currentPlayer) {
            super(currentPlayer);
        }

        @Override
        public void onGroupMembershipRequested(CollarClient client, GroupMembershipRequest resp) {
            super.onGroupMembershipRequested(client, resp);
            try {
                client.acceptGroupRequest(resp.groupId, Group.MembershipState.ACCEPTED);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onGroupJoined(CollarClient client, ServerMessage.AcceptGroupMembershipResponse acceptGroupMembershipResponse) {
            super.onGroupJoined(client, acceptGroupMembershipResponse);

            try {
                client.updatePosition(createPosition());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onGroupCreated(CollarClient client, ServerMessage.CreateGroupResponse resp) {
            super.onGroupCreated(client, resp);
            try {
                client.updatePosition(createPosition());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onGroupUpdated(CollarClient client, ServerMessage.UpdatePlayerStateResponse updatePlayerStateResponse) {
            super.onGroupUpdated(client, updatePlayerStateResponse);
            try {
                client.updatePosition(createPosition());
            } catch (IOException e) {
                e.printStackTrace();
            }

            updatePlayerStateResponse.groups.forEach(group -> {
                if (group.members.size() == 1) {
                    try {
                        client.leaveGroup(group);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @NotNull
    private static Position createPosition() {
        Random rd = new Random();
        return new Position(rd.nextDouble(), rd.nextDouble(), rd.nextDouble(), 0);
    }


    public static class Listener2 extends AbstractCollarListener {

        private final UUID player1;
        private int count = 0;

        public Listener2(UUID currentPlayer, UUID player1) {
            super(currentPlayer);
            this.player1 = player1;
        }

        @Override
        public void onSessionCreated(CollarClient client) {
            super.onSessionCreated(client);
            try {
                client.createGroup(ImmutableList.of(player1), createPosition());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onGroupCreated(CollarClient client, ServerMessage.CreateGroupResponse resp) {
            super.onGroupCreated(client, resp);
            try {
                client.updatePosition(createPosition());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onGroupUpdated(CollarClient client, ServerMessage.UpdatePlayerStateResponse updatePlayerStateResponse) {
            super.onGroupUpdated(client, updatePlayerStateResponse);
            try {
                client.updatePosition(createPosition());
            } catch (IOException e) {
                e.printStackTrace();
            }
            count++;
            if (count >= 10) {
                updatePlayerStateResponse.groups.forEach(group -> {
                    try {
                        client.leaveGroup(group);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        @Override
        public void onGroupLeft(CollarClient client, ServerMessage.LeaveGroupResponse resp) {
            super.onGroupLeft(client, resp);
            client.disconnect();
        }
    }

    public abstract static class AbstractCollarListener implements CollarListener {
        private final UUID currentPlayer;

        public AbstractCollarListener(UUID currentPlayer) {
            this.currentPlayer = currentPlayer;
        }

        @Override
        public void onSessionCreated(CollarClient client) {
            System.out.println(getPlayerPrefix() + "onSessionCreated");
            waitABit();
        }

        @Override
        public void onDisconnect(CollarClient client) {
            System.out.println(getPlayerPrefix() + "onDisconnect");
            waitABit();
        }

        @Override
        public void onGroupCreated(CollarClient client, ServerMessage.CreateGroupResponse resp) {
            System.out.println(getPlayerPrefix() + "onGroupCreated " + printGroup(resp.group));
            waitABit();
        }

        @Override
        public void onGroupMembershipRequested(CollarClient client, GroupMembershipRequest resp) {
            System.out.println(getPlayerPrefix() + "onGroupMembershipRequested " + resp.groupId);
            waitABit();
        }

        @Override
        public void onGroupJoined(CollarClient client, ServerMessage.AcceptGroupMembershipResponse acceptGroupMembershipResponse) {
            System.out.println(getPlayerPrefix() + "onGroupJoined " + printGroup(acceptGroupMembershipResponse.group));
            waitABit();
        }

        @Override
        public void onGroupLeft(CollarClient client, ServerMessage.LeaveGroupResponse resp) {
            System.out.println(getPlayerPrefix() + "onGroupLeft");
            waitABit();
        }

        @Override
        public void onGroupUpdated(CollarClient client, ServerMessage.UpdatePlayerStateResponse updatePlayerStateResponse) {
            StringBuilder sb = new StringBuilder();
            for (Group group : updatePlayerStateResponse.groups) {
                sb.append(printGroup(group));
            }
            System.out.println(getPlayerPrefix() + "onGroupUpdated" + sb);
            waitABit();
        }

        private String printGroup(Group group) {
            StringWriter s = new StringWriter();
            PrintWriter writer = new PrintWriter(s);
            writer.print(getPlayerPrefix() + "Group ");
            writer.println(group.id);
            group.members.values().stream().map(value -> getPlayerPrefix() + value.player + " " + value.membershipState + " " + Position.toString(value.location)).forEach(writer::println);
            writer.flush();
            return s.toString();
        }

        private String getPlayerPrefix() {
            return "[" + currentPlayer + "] ";
        }
    }

    private static void waitABit() {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
