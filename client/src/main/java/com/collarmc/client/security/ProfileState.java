package com.collarmc.client.security;

import com.collarmc.client.HomeDirectory;
import com.collarmc.io.AtomicFile;
import com.collarmc.utils.Utils;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Holds state about the authorized identity's Collar profile
 */
public final class ProfileState {
    @JsonProperty("owner")
    public final UUID owner;

    public ProfileState(@JsonProperty("owner") UUID owner) {
        this.owner = owner;
    }

    public void write(HomeDirectory home) {
        try {
            AtomicFile.write(getProfileFile(home), file -> Utils.messagePackMapper().writeValue(file, this));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static ProfileState read(HomeDirectory home)  {
        try {
            return Utils.messagePackMapper().readValue(getProfileFile(home), ProfileState.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean exists(HomeDirectory home) {
        try {
            return getProfileFile(home).exists();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static File getProfileFile(HomeDirectory home) throws IOException {
        return new File(home.profile(), "profile");
    }
}
