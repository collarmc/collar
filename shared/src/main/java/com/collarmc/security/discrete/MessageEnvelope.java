package com.collarmc.security.discrete;

import java.nio.ByteBuffer;

public class MessageEnvelope {
    public final byte[] signature;
    public final byte[] contents;

    public MessageEnvelope(byte[] signature, byte[] contents) {
        this.signature = signature;
        this.contents = contents;
    }

    public MessageEnvelope(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        this.signature = new byte[buffer.getInt()];
        buffer.get(this.signature);
        this.contents = new byte[buffer.getInt()];
        buffer.get(this.contents);
    }

    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(Short.MAX_VALUE);
        buffer.putInt(signature.length);
        buffer.put(signature);
        buffer.putInt(contents.length);
        buffer.put(contents);
        return buffer.array();
    }
}
