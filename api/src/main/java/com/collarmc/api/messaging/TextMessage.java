package com.collarmc.api.messaging;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A simple text message
 */
public final class TextMessage implements Message {

    public static final int MAX_LENGTH = 2000;

    /**
     * The message that is sent to the recipient
     */
    @JsonProperty("content")
    public final String content;

    /**
     * The message that is printed for the sender immediately after delivery
     */
    @JsonIgnore
    public final String consoleMessage;

    @JsonCreator
    public TextMessage(@JsonProperty("content") String content) {
        checkContentLength(content);
        this.content = content;
        this.consoleMessage = null;
    }

    public TextMessage(String content, String consoleMessage) {
        checkContentLength(content);
        this.content = content;
        this.consoleMessage = consoleMessage;
    }

    @Override
    public String toString() {
        return "TextMessage '" + content + "'";
    }

    private static void checkContentLength(String content) {
        if (content.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("content exceeds " + MAX_LENGTH + " characters");
        }
    }
}
