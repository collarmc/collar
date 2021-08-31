package com.collarmc.security.messages;

import java.nio.ByteBuffer;

/**
 * Holds a message signed with the sender's signature key
 */
public final class SignedMessage {
    private static final int VERSION = 1;
    public final byte[] signature;
    public final byte[] contents;

    public SignedMessage(byte[] signature, byte[] contents) {
        this.signature = signature;
        this.contents = contents;
    }

    public SignedMessage(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int version = buffer.getInt();
        if (version != VERSION) {
            throw new IllegalStateException("unknown version " + version);
        }
        this.signature = new byte[buffer.getInt()];
        buffer.get(this.signature);
        this.contents = new byte[buffer.getInt()];
        buffer.get(this.contents);
    }

    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate((3*4) + signature.length + contents.length);
        buffer.putInt(VERSION);
        buffer.putInt(signature.length);
        buffer.put(signature);
        buffer.putInt(contents.length);
        buffer.put(contents);
        return buffer.array();
    }
}
