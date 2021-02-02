package team.catgirl.collar.client;

import com.google.common.base.MoreObjects;
import team.catgirl.collar.api.location.Position;
import team.catgirl.collar.security.mojang.MinecraftSession;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CollarConfiguration {

    private static final Logger LOGGER = Logger.getLogger(CollarConfiguration.class.getName());

    public final Supplier<Position> playerPosition;
    public final Supplier<MinecraftSession> sessionSupplier;
    public final HomeDirectory homeDirectory;
    public final URL collarServerURL;
    public final CollarListener listener;

    private CollarConfiguration(Supplier<Position> playerPosition, Supplier<MinecraftSession> sessionSupplier, HomeDirectory homeDirectory, URL collarServerURL, CollarListener listener) {
        this.playerPosition = playerPosition;
        this.sessionSupplier = sessionSupplier;
        this.homeDirectory = homeDirectory;
        this.collarServerURL = collarServerURL;
        this.listener = listener;
    }

    public final static class Builder {
        private CollarListener listener;
        private Supplier<Position> playerPosition;
        private Supplier<MinecraftSession> sessionSupplier;
        private File homeDirectory;
        private URL collarServerURL;

        public Builder() {}

        public Builder withListener(CollarListener listener) {
            this.listener = listener;
            return this;
        }

        public Builder withCollarServer(URL collarServerURL) {
            this.collarServerURL = collarServerURL;
            return this;
        }

        public Builder withCollarServer(String collarServerURL) {
            try {
                this.collarServerURL = new URL(collarServerURL);
            } catch (MalformedURLException e) {
                throw new IllegalStateException(e);
            }
            return this;
        }

        public Builder withHomeDirectory(File homeDirectory) {
            this.homeDirectory = homeDirectory;
            return this;
        }

        public Builder withPlayerPosition(Supplier<Position> playerPosition) {
            this.playerPosition = playerPosition;
            return this;
        }

        public Builder withNoJangAuthentication(UUID uuid, String server) {
            this.sessionSupplier = () -> MinecraftSession.from(uuid, null,  null, server);
            return this;
        }

        public Builder withMojangAuthentication(Supplier<MinecraftSession> mojangAuthentication) {
            this.sessionSupplier = mojangAuthentication;
            return this;
        }

        public CollarConfiguration build() throws IOException {
            Objects.requireNonNull(listener, "Collar listener was not set");
            Objects.requireNonNull(collarServerURL, "Collar server URL must be set");
            Objects.requireNonNull(homeDirectory, "Minecraft home directory must be set");
            Objects.requireNonNull(playerPosition, "Player Position supplier not set");
            Objects.requireNonNull(sessionSupplier, "Session supplier not set");
            HomeDirectory from = HomeDirectory.from(homeDirectory, collarServerURL.getHost());
            return new CollarConfiguration(MoreObjects.firstNonNull(playerPosition, () -> {
                LOGGER.log(Level.WARNING, "Location features are disabled. Consumer did not provide a player position supplier");
                return Position.UNKNOWN;
            }), sessionSupplier, from, collarServerURL, listener);
        }
    }
}
