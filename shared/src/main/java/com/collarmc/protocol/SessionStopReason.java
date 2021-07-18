package com.collarmc.protocol;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.google.common.base.MoreObjects;

public enum SessionStopReason {
    NORMAL_CLOSE(1000, "Session ended"),
    TOO_MANY_REQUESTS(1013, "Too many requests"),
    UNAUTHORISED(1401, "Unauthorised"),
    @JsonEnumDefaultValue
    SERVER_ERROR(1500, "Server error");

    public final int code;
    private final String reason;

    SessionStopReason(int code, String reason) {
        this.code = code;
        this.reason = reason;
    }

    public String message(String alternateReason) {
        return MoreObjects.firstNonNull(alternateReason, reason);
    }
}
