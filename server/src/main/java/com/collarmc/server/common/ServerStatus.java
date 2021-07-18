package com.collarmc.server.common;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class ServerStatus {
    @JsonProperty("playerCount")
    public final long playerCount;
    @JsonProperty("playersOnline")
    public final long playersOnline;

    public ServerStatus(long playerCount, long playersOnline) {
        this.playerCount = playerCount;
        this.playersOnline = playersOnline;
    }
}
