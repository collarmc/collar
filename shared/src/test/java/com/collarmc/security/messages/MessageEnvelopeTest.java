package com.collarmc.security.messages;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class MessageEnvelopeTest {
    @Test
    public void serialize() {
        MessageEnvelope messageEnvelope = new MessageEnvelope("key".getBytes(StandardCharsets.UTF_8), "contents".getBytes(StandardCharsets.UTF_8));
        byte[] bytes = messageEnvelope.serialize();
        MessageEnvelope deserialized = new MessageEnvelope(bytes);
        Assert.assertEquals("key", new String(deserialized.signature, StandardCharsets.UTF_8));
        Assert.assertEquals("contents", new String(deserialized.contents, StandardCharsets.UTF_8));
    }
}
