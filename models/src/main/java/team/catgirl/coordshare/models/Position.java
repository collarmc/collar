package team.catgirl.coordshare.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public final class Position {
    @JsonProperty("x")
    public final Double x;
    @JsonProperty("y")
    public final Double y;
    @JsonProperty("z")
    public final Double z;

    public Position(@JsonProperty("x") Double x, @JsonProperty("y") Double y, @JsonProperty("z") Double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static String toString(Position location) {
        return location == null ? "" : location.toString();
    }

    @Override
    public String toString() {
        return "[" + x + "," + y + "," + z + "]";
    }
}

