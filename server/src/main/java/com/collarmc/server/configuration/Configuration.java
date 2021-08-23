package com.collarmc.server.configuration;

import com.collarmc.http.HttpClient;
import com.collarmc.server.http.AppUrlProvider;
import com.collarmc.server.http.CollarWebAppUrlProvider;
import com.collarmc.server.http.DefaultAppUrlProvider;
import com.collarmc.server.mail.Email;
import com.collarmc.server.mail.LocalEmail;
import com.collarmc.server.mail.MailGunEmail;
import com.collarmc.server.mongo.Mongo;
import com.collarmc.server.security.hashing.PasswordHashing;
import com.collarmc.server.security.mojang.MinecraftSessionVerifier;
import com.collarmc.server.security.mojang.MojangMinecraftSessionVerifier;
import com.collarmc.server.security.mojang.NojangMinecraftSessionVerifier;
import com.collarmc.server.services.authentication.TokenCrypter;
import com.mongodb.client.MongoDatabase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Configuration {
    private static final Logger LOGGER = LogManager.getLogger(Configuration.class.getName());

    public final MongoDatabase database;
    public final AppUrlProvider appUrlProvider;
    public final TokenCrypter tokenCrypter;
    public final PasswordHashing passwordHashing;
    public final MinecraftSessionVerifier minecraftSessionVerifier;
    public final String corsOrigin;
    public final boolean enableWeb;
    public final int httpPort;
    public final Email email;
    public final HttpClient http;

    public Configuration(MongoDatabase database,
                         AppUrlProvider appUrlProvider,
                         TokenCrypter tokenCrypter,
                         PasswordHashing passwordHashing,
                         MinecraftSessionVerifier minecraftSessionVerifier,
                         String corsOrigin,
                         boolean enableWeb,
                         int httpPort,
                         Email email,
                         HttpClient http
    ) {
        this.database = database;
        this.appUrlProvider = appUrlProvider;
        this.tokenCrypter = tokenCrypter;
        this.passwordHashing = passwordHashing;
        this.minecraftSessionVerifier = minecraftSessionVerifier;
        this.corsOrigin = corsOrigin;
        this.enableWeb = enableWeb;
        this.httpPort = httpPort;
        this.email = email;
        this.http = http;
        LOGGER.info("Using Email type " + email.getClass().getSimpleName());
    }

    public static Configuration fromEnvironment() {
        String baseUrl = System.getenv("COLLAR_APP_URL");
        if (baseUrl == null) {
            throw new IllegalStateException("COLLAR_APP_URL not set");
        }
        String crypterPassword = System.getenv("COLLAR_CRYPTER_PASSWORD");
        if (crypterPassword == null) {
            throw new IllegalStateException("COLLAR_CRYPTER_PASSWORD not set");
        }
        String passwordSalt = System.getenv("COLLAR_PASSWORD_SALT");
        if (passwordSalt == null) {
            throw new IllegalStateException("COLLAR_PASSWORD_SALT not set");
        }
        String verifyMojangSessions = System.getenv("COLLAR_VERIFY_MOJANG_SESSIONS");
        if (verifyMojangSessions == null) {
            throw new IllegalStateException("COLLAR_VERIFY_MOJANG_SESSIONS not set");
        }
        boolean useMojang = Boolean.parseBoolean(verifyMojangSessions);
        boolean enableWeb = Boolean.parseBoolean(System.getenv("COLLAR_ENABLED_WEB"));
        String mailgunDomain = System.getenv("MAILGUN_DOMAIN");
        if (mailgunDomain == null) {
            throw new IllegalStateException("MAILGUN_DOMAIN not set");
        }
        String mailgunApiKey = System.getenv("MAILGUN_API_KEY");
        if (mailgunApiKey == null) {
            throw new IllegalStateException("MAILGUN_API_KEY not set");
        }
        AppUrlProvider appUrlProvider = new CollarWebAppUrlProvider(baseUrl);
        HttpClient http = new HttpClient(null);
        return new Configuration(
                Mongo.database(),
                appUrlProvider,
                new TokenCrypter(crypterPassword),
                new PasswordHashing(passwordSalt),
                useMojang ? new MojangMinecraftSessionVerifier(http) : new NojangMinecraftSessionVerifier(),
                appUrlProvider.homeUrl(),
                enableWeb,
                httpPort(),
                new MailGunEmail(http, appUrlProvider, mailgunDomain, mailgunApiKey),
                http
        );
    }

    public static Configuration defaultConfiguration() {
        LOGGER.error("Starting in insecure development mode. Do not use in production.");
        DefaultAppUrlProvider appUrlProvider = new DefaultAppUrlProvider("http://localhost:3000");
        return new Configuration(
                Mongo.database("mongodb://localhost/collar-dev"),
                appUrlProvider,
                new TokenCrypter("insecureTokenCrypterPassword"),
                new PasswordHashing("VSZL*bR8-=r]r5P_"),
                new NojangMinecraftSessionVerifier(),
                "*",
                true,
                httpPort(),
                new LocalEmail(appUrlProvider),
                new HttpClient(null));
    }

    public static Configuration testConfiguration(MongoDatabase db, MinecraftSessionVerifier sessionVerifier) {
        LOGGER.error("Starting in insecure testing mode. Do not use in production.");
        DefaultAppUrlProvider appUrlProvider = new DefaultAppUrlProvider("http://localhost:3001");
        return new Configuration(
                db,
                appUrlProvider,
                new TokenCrypter("insecureTokenCrypterPassword"),
                new PasswordHashing("VSZL*bR8-=r]r5P_"),
                sessionVerifier,
                "*",
                false,
                3001,
                new LocalEmail(appUrlProvider),
                new HttpClient(null));
    }

    private static int httpPort() {
        String portValue = System.getenv("PORT");
        return portValue != null ? Integer.parseInt(portValue) : 4000;
    }
}
