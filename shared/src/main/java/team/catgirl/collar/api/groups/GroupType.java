package team.catgirl.collar.api.groups;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum GroupType {
    /**
     * A persistent group of players
     */
    GROUP("group", "groups"),
    /**
     * A temporary group of players
     */
    @JsonEnumDefaultValue
    PARTY("party", "parties"),
    /**
     * A location proximity group
     */
    NEARBY("nearby", "nearby groups");

    public final String name;
    public final String plural;

    GroupType(String name, String plural) {
        this.name = name;
        this.plural = plural;
    }
}
