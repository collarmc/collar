package com.collarmc.client.api.location;

import com.collarmc.client.minecraft.Ticks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

class LocationUpdater implements Ticks.TickListener {

    private static final Logger LOGGER = LogManager.getLogger(LocationUpdater.class);

    private final LocationApi locationApi;
    private final Ticks ticks;
    private final AtomicInteger tickCounter = new AtomicInteger();

    public LocationUpdater(LocationApi locationApi, Ticks ticks) {
        this.locationApi = locationApi;
        this.ticks = ticks;
    }

    public boolean isRunning() {
        return ticks.isSubscribed(this);
    }

    public void start() {
        ticks.subscribe(this);
        LOGGER.info("Started sending player location");
    }

    public void stop() {
        ticks.unsubscribe(this);
        LOGGER.info("Stopped sending player location");
    }

    @Override
    public void onTick() {
        if (tickCounter.incrementAndGet() % 10 == 0) {
            LOGGER.debug("Location published");
            locationApi.publishLocation();
        }
    }
}
