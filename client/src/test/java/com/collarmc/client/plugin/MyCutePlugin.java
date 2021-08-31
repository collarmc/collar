package com.collarmc.client.plugin;

import com.collarmc.client.api.plugin.CollarPlugin;
import com.collarmc.client.api.plugin.CollarPluginLoadedEvent;
import com.collarmc.pounce.EventBus;

public class MyCutePlugin implements CollarPlugin {
    public boolean loaded;
    public EventBus eventBus;

    @Override
    public void onLoad(CollarPluginLoadedEvent event) {
        loaded = true;
        eventBus = event.eventBus;
    }
}
