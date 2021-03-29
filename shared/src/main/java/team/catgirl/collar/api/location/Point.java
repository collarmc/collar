package team.catgirl.collar.api.location;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public final class Point {

	public static final Point NONE = new Point(0, 0);

	@JsonProperty("x")
	public final int x;
	@JsonProperty("y")
	public final int y;
	
	public Point(@JsonProperty("x") int x, @JsonProperty("y") int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Point point = (Point) o;
		return x == point.x && y == point.y;
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y);
	}
}
