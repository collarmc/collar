package com.collarmc.client.api.location;

import com.collarmc.client.minecraft.Ticks;

import java.util.concurrent.atomic.AtomicInteger;

class LocationUpdater implements Ticks.TickListener {
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
    }

    public void stop() {
        ticks.unsubscribe(this);
    }

    @Override
    public void onTick() {
        if (tickCounter.incrementAndGet() % 10 == 0) {
            locationApi.publishLocation();
        }
    }
}
