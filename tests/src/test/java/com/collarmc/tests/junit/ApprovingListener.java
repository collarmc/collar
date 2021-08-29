package com.collarmc.tests.junit;

import com.collarmc.api.http.RequestContext;
import com.collarmc.api.profiles.Profile;
import com.collarmc.api.profiles.ProfileService;
import com.collarmc.client.events.ConfirmClientRegistrationEvent;
import com.collarmc.pounce.EventBus;
import com.collarmc.pounce.Preference;
import com.collarmc.pounce.Subscribe;
import com.collarmc.server.Services;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Approves the device on initial startup of client
 **/
public class ApprovingListener {
    private final UUID profile;
    private final Services services;
    private final AtomicInteger devicesConfirmed;

    public ApprovingListener(UUID profile, Services services, EventBus eventBus) {
        eventBus.subscribe(this);
        this.profile = profile;
        this.services = services;
        this.devicesConfirmed = new AtomicInteger(0);
    }

    @Subscribe(Preference.CALLER)
    public void onConfirmDeviceRegistration(ConfirmClientRegistrationEvent event) {
        Profile profile = services.profiles.getProfile(RequestContext.SERVER, ProfileService.GetProfileRequest.byId(this.profile)).profile;
        services.deviceRegistration.onClientRegistered(profile.toPublic(), event.token);
        devicesConfirmed.incrementAndGet();
    }
}
