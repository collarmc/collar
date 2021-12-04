package com.collarmc.security.messages;

import com.collarmc.api.io.IO;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class GroupMessageEnvelope {
    private static final int VERSION = 1;

    public final Map<UUID, GroupMessage> messages;

    public GroupMessageEnvelope(List<GroupMessage> messages) {
        this.messages = map(messages);
    }

    public GroupMessageEnvelope(byte[] bytes) {
        List<GroupMessage> messages = new ArrayList<>();
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        try (DataInputStream dis = new DataInputStream(bis)) {
            int version = dis.readInt();
            if (version != VERSION) {
                throw new IllegalStateException("unknown version " + VERSION);
            }
            int messageCount = dis.readInt();
            for (int i = 0; i < messageCount; i++) {
                messages.add(new GroupMessage(IO.readBytes(dis)));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        this.messages = map(messages);
    }

    public byte[] serialize() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(bos)) {
            dos.writeInt(VERSION);
            dos.writeInt(messages.size());
            for (GroupMessage groupMessage : messages.values()) {
                IO.writeBytes(dos, groupMessage.serialize());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return bos.toByteArray();
    }

    private static Map<UUID, GroupMessage> map(List<GroupMessage> messages) {
        return messages.stream().collect(Collectors.toMap(groupMessage -> groupMessage.recipient, groupMessage -> groupMessage));
    }
}
