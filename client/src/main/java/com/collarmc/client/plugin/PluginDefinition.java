package com.collarmc.client.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Defines a Collar plugin
 */
public final class PluginDefinition {
    @JsonProperty("id")
    public final String id;
    @JsonProperty("entrypoints")
    public final Map<String, String> entrypoints;

    public PluginDefinition(@JsonProperty("id") String id,
                            @JsonProperty("entrypoints") Map<String, String> entrypoints) {
        this.id = id;
        this.entrypoints = entrypoints;
    }
}
