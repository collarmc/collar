package com.collarmc.security.messages;

import com.collarmc.io.IO;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class GroupMessage {
    private static final int VERSION = 1;
    public final UUID recipient;
    public final byte[] contents;

    public GroupMessage(UUID recipient, byte[] contents) {
        this.recipient = recipient;
        this.contents = contents;
    }

    public GroupMessage(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int version = buffer.getInt();
        if (version != VERSION) {
            throw new IllegalStateException("unknown version " + version);
        }
        byte[] recipientIdBytes = new byte[16];
        buffer.get(recipientIdBytes);
        this.recipient = IO.readUUIDFromBytes(recipientIdBytes);
        this.contents = new byte[buffer.getInt()];
        buffer.get(contents);
    }

    public byte[] serialize() {
        // TODO: what is the max size of a group message?
        ByteBuffer buffer = ByteBuffer.allocate(16 + contents.length + (4*2));
        buffer.putInt(VERSION);
        buffer.put(IO.writeUUIDToBytes(recipient));
        buffer.putInt(contents.length);
        buffer.put(contents);
        return buffer.array();
    }
}
