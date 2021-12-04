package com.collarmc.api.location;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum Dimension {
    OVERWORLD,
    NETHER,
    END,
    @JsonEnumDefaultValue
    UNKNOWN
}
