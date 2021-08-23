package com.collarmc.security.messages;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class GroupMessageTest {
    @Test
    public void serialize() {
        UUID recipient = UUID.randomUUID();
        GroupMessage message = new GroupMessage(recipient, "contents".getBytes(StandardCharsets.UTF_8));
        byte[] bytes = message.serialize();
        GroupMessage deserialized = new GroupMessage(bytes);
        Assert.assertEquals(recipient, deserialized.recipient);
        Assert.assertEquals("contents", new String(deserialized.contents, StandardCharsets.UTF_8));
    }
}
