package com.collarmc.security.messages;

import com.collarmc.io.IO;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * A single group message. Packed inside a {@link GroupMessageEnvelope}.
 */
public final class GroupMessage {
    public final UUID group;
    public final byte[] sessionId;
    public final byte[] contents;

    public GroupMessage(UUID group,
                        byte[] sessionId,
                        byte[] contents) {
        this.group = group;
        this.sessionId = sessionId;
        this.contents = contents;
    }

    public GroupMessage(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        byte[] uuid = new byte[16];
        buffer.get(uuid);
        this.group = IO.readUUIDFromBytes(uuid);
        this.sessionId = new byte[buffer.getInt()];
        buffer.get(sessionId);
        this.contents = new byte[buffer.getInt()];
        buffer.get(contents);
    }

    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(16 + sessionId.length + contents.length + (2*4));
        buffer.put(IO.writeUUIDToBytes(group));
        buffer.putInt(sessionId.length);
        buffer.put(sessionId);
        buffer.putInt(contents.length);
        buffer.put(contents);
        return buffer.array();
    }
}
