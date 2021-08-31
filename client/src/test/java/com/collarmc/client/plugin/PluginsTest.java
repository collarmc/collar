package com.collarmc.client.plugin;

import com.collarmc.client.api.plugin.CollarPlugin;
import com.collarmc.pounce.EventBus;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class PluginsTest {
    @Test
    public void plugins() throws Exception {
        Plugins plugins = new Plugins();
        EventBus eventBus = new EventBus(Runnable::run);
        plugins.loadPlugins(PluginsTest.class.getClassLoader(), eventBus);
        Optional<CollarPlugin> collarPlugin = plugins.find("MyModId", "MyCollarPlugin");
        Assert.assertTrue(collarPlugin.isPresent());

        MyCutePlugin cutePlugin = (MyCutePlugin) collarPlugin.get();
        Assert.assertTrue(cutePlugin.loaded);
        Assert.assertEquals(eventBus, cutePlugin.eventBus);

        Optional<CollarPlugin> notFoundById = plugins.find("MyModId111", "MyCollarPlugin");
        Assert.assertFalse(notFoundById.isPresent());

        Optional<CollarPlugin> notFoundByPlugin = plugins.find("MyModId", "MyCollarPlugin222");
        Assert.assertFalse(notFoundByPlugin.isPresent());
    }
}
