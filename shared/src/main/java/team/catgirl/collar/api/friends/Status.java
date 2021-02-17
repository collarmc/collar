package team.catgirl.collar.api.friends;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum Status {
    ONLINE,
    OFFLINE,
    @JsonEnumDefaultValue
    UNKNOWN
}
