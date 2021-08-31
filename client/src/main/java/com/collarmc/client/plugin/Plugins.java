package com.collarmc.client.plugin;

import com.collarmc.client.api.plugin.CollarPlugin;
import com.collarmc.client.api.plugin.CollarPluginLoadedEvent;
import com.collarmc.pounce.EventBus;
import com.collarmc.utils.Utils;
import com.google.common.collect.Multimap;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plugin loader. For use in API mod only.
 */
public final class Plugins {

    private static final Logger LOGGER = LogManager.getLogger(Plugins.class);

    private static final Map<String, Map<String, CollarPlugin>> plugins = new ConcurrentHashMap<>();

    /**
     * Find a collar plugin
     * @param modId of mod
     * @param entryPointId of plugin
     * @return plugin
     */
    public Optional<CollarPlugin> find(String modId, String entryPointId) {
        Map<String, CollarPlugin> plugins = Plugins.plugins.get(modId);
        if (plugins == null) {
            return Optional.empty();
        }
        CollarPlugin collarPlugin = plugins.get(entryPointId);
        return Optional.ofNullable(collarPlugin);
    }

    /**
     * Load plugins
     * @param classLoader to search for collar.plugin.json files
     * @param eventBus for plugins
     * @throws IOException problem loading from classpath
     */
    public void loadPlugins(ClassLoader classLoader, EventBus eventBus) throws IOException {
        Enumeration<URL> resources = classLoader.getResources("collar.plugin.json");
        while (resources.hasMoreElements()) {
            URL pluginUrl = resources.nextElement();
            try {
                PluginDefinition pluginDefinition = Utils.jsonMapper().readValue(pluginUrl, PluginDefinition.class);
                pluginDefinition.entrypoints.forEach((key, clazz) -> {
                    loadPlugin(classLoader, eventBus, clazz).ifPresent(collarPlugin -> {
                        plugins.compute(pluginDefinition.id, (s, pluginMap) -> {
                            pluginMap = pluginMap == null ? new ConcurrentHashMap<>() : pluginMap;
                            pluginMap.putIfAbsent(key, collarPlugin);
                            return pluginMap;
                        });
                    });
                });
            } catch (IOException e) {
                LOGGER.error("Failed to Collar load plugin definition " + pluginUrl, e);
            }
        }
    }

    private Optional<CollarPlugin> loadPlugin(ClassLoader classLoader, EventBus eventBus, String clazz) {
        Class<?> aClass;
        try {
            aClass = classLoader.loadClass(clazz);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Could not load Collar plugin class " + clazz);
            return Optional.empty();
        }
        if (!CollarPlugin.class.isAssignableFrom(aClass)) {
            LOGGER.error("Collar plugin class " + clazz + " is not assignable from " + CollarPlugin.class);
            return Optional.empty();
        }
        Constructor<?> constructor;
        try {
            constructor = aClass.getConstructor();
        } catch (NoSuchMethodException e) {
            LOGGER.error("Collar plugin class " + clazz + " did not have a public no-args constructor");
            return Optional.empty();
        }
        // Create the plugin
        CollarPlugin plugin;
        try {
            plugin = (CollarPlugin) constructor.newInstance();
        } catch (InstantiationException e) {
            LOGGER.error("Collar plugin class " + clazz + " failed to be instantiated", e);
            return Optional.empty();
        } catch (IllegalAccessException e) {
            LOGGER.error("Collar plugin class " + clazz + " constructor was not public");
            return Optional.empty();
        } catch (InvocationTargetException e) {
            LOGGER.error("Collar plugin class " + clazz + " failed to be invoked", e);
            return Optional.empty();
        }
        // Load the plugin and subscribe to event bus
        try {
            plugin.onLoad(new CollarPluginLoadedEvent(eventBus));
            eventBus.subscribe(plugin);
        } catch (Throwable e) {
            LOGGER.error("Collar plugin class " + clazz + " failed to be loaded", e);
        }
        return Optional.of(plugin);
    }
}
