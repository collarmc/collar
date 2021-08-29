package com.collarmc.client.api.location.events;

import com.collarmc.api.location.Location;
import com.collarmc.api.session.Player;
import com.collarmc.client.Collar;
import com.collarmc.client.events.AbstractCollarEvent;

/**
 * Fired when a player location was updated
 */
public final class LocationUpdatedEvent extends AbstractCollarEvent {
    public final Player player;
    public final Location location;

    public LocationUpdatedEvent(Collar collar, Player player, Location location) {
        super(collar);
        this.player = player;
        this.location = location;
    }
}
