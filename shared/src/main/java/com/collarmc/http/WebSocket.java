package com.collarmc.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket handle to send data through the socket
 */
public final class WebSocket {
    /**
     * Request used to create the WebSocket
     */
    public final Request request;
    private final Channel channel;
    private AtomicInteger sendingCount = new AtomicInteger();
    private volatile boolean closing = false;

    WebSocket(Request request, Channel channel) {
        this.request = request;
        this.channel = channel;
    }

    /**
     * Send message to server
     * @param message to send
     */
    public void send(ByteBuffer message) {
        assertWritable();
        sendingCount.incrementAndGet();
        channel.writeAndFlush(
                new BinaryWebSocketFrame(Unpooled.copiedBuffer(message)),
                channel.newPromise().addListener(future1 -> sendingCount.decrementAndGet())
        );
    }

    private void assertWritable() {
        if (closing) {
            throw new IllegalStateException("socket is closing");
        }
        if (!channel.isWritable()) {
            throw new IllegalStateException("socket is closed");
        }
    }

    /**
     * Close the socket after in flight messages have been sent
     * @param code as reason
     * @param reason message
     */
    public void close(int code, String reason) {
        waitForSendingToComplete();
        sendingCount.getAndIncrement();
        if (channel.isWritable()) {
            channel.writeAndFlush(
                    new CloseWebSocketFrame(code, reason),
                    channel.newPromise().addListener(future1 -> sendingCount.decrementAndGet())
            );
        }
    }

    /**
     * Close the socket after in flight messages have been sent
     */
    public void close() {
        waitForSendingToComplete();
        channel.close();
    }

    private void waitForSendingToComplete() {
        closing = true;
        while (sendingCount.get() != 0) {
            try {
                Thread.sleep(TimeUnit.MILLISECONDS.toMillis(10));
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
