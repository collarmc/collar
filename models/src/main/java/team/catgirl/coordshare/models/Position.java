package team.catgirl.coordshare.models;

import com.fasterxml.jackson.annotation.JsonProperty;

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
}

