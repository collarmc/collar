package com.collarmc.client.events;

import com.collarmc.client.Collar;
import com.collarmc.security.mojang.MinecraftSession;

/**
 * Fired when the server could not validate the minecraft session
 */
public final class MinecraftAccountVerificationFailedEvent extends AbstractCollarEvent {
    public final MinecraftSession minecraftSession;

    public MinecraftAccountVerificationFailedEvent(Collar collar, MinecraftSession minecraftSession) {
        super(collar);
        this.minecraftSession = minecraftSession;
    }
}
