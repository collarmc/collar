package com.collarmc.security.messages;

import com.collarmc.io.IO;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class GroupMessage {
    public final UUID recipient;
    public final byte[] contents;

    public GroupMessage(UUID recipient, byte[] contents) {
        this.recipient = recipient;
        this.contents = contents;
    }

    public GroupMessage(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        byte[] recipientIdBytes = new byte[16];
        buffer.get(recipientIdBytes);
        this.recipient = IO.readUUIDFromBytes(recipientIdBytes);
        this.contents = new byte[buffer.getInt()];
        buffer.get(contents);
    }

    public byte[] serialize() {
        // TODO: what is the max size of a group message?
        ByteBuffer buffer = ByteBuffer.allocate(16 + contents.length + 4);
        buffer.put(IO.writeUUIDToBytes(recipient));
        buffer.putInt(contents.length);
        buffer.put(contents);
        return buffer.array();
    }
}
