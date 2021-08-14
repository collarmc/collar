package com.collarmc.security.discrete;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class GroupMessageTest {
    @Test
    public void serialize() {
        UUID id = UUID.randomUUID();
        GroupMessage message = new GroupMessage(id, "session".getBytes(StandardCharsets.UTF_8), "contents".getBytes(StandardCharsets.UTF_8));
        byte[] bytes = message.serialize();
        GroupMessage deserialized = new GroupMessage(bytes);
        Assert.assertEquals(id, deserialized.group);
        Assert.assertEquals("session", new String(deserialized.sessionId, StandardCharsets.UTF_8));
        Assert.assertEquals("contents", new String(deserialized.contents, StandardCharsets.UTF_8));
    }
}
