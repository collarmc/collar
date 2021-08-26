package com.collarmc.security.messages;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class SignedMessageTest {
    @Test
    public void serialize() {
        SignedMessage signedMessage = new SignedMessage("key".getBytes(StandardCharsets.UTF_8), "contents".getBytes(StandardCharsets.UTF_8));
        byte[] bytes = signedMessage.serialize();
        SignedMessage deserialized = new SignedMessage(bytes);
        Assert.assertEquals("key", new String(deserialized.signature, StandardCharsets.UTF_8));
        Assert.assertEquals("contents", new String(deserialized.contents, StandardCharsets.UTF_8));
    }
}
