package com.collarmc.tools;

import com.collarmc.api.authentication.AuthenticationService;
import com.collarmc.api.authentication.AuthenticationService.LoginRequest;
import com.collarmc.api.authentication.AuthenticationService.LoginResponse;
import com.collarmc.api.profiles.Profile;
import com.collarmc.api.profiles.ProfileService.GetProfileRequest;
import com.collarmc.http.HttpClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.collarmc.api.profiles.ProfileService.UpdateProfileRequest.resetKeys;

public final class AdminTool {

    private final HttpClient http;
    private CollarApi client;
    private String configName;

    public AdminTool(HttpClient http) {
        this.http = http;
    }

    public int currentConfig(String configName) {
        Map<String, String> config = loadConfig(configName);
        if (config.isEmpty()) {
            return -1;
        }
        this.configName = configName;
        client = new CollarApi(config.get("apiUrl") + "/api/1/", http);
        LoginResponse response = client.login(new LoginRequest(config.get("email"), config.get("password")));
        client.setToken(response.token);
        System.out.println("logged in as " + response.profile.name);
        return 1;
    }

    public String currentConfig() {
        return configName;
    }

    public void resetPassword(String email) {
        client.resetPassword(new AuthenticationService.RequestPasswordResetRequest(email));
        System.out.println("Reset password request sent for " + email);
    }

    public void resetIdentity(String email) {
        Profile profile = client.getProfile(GetProfileRequest.byEmail(email)).profile;
        client.updateProfile(resetKeys(profile.id));
        System.out.println("Reset identity for " + email);
    }

    public int addConfig(String name, String apiUrl, String email, String password) {
        Properties properties = loadProperties();
        if (loadConfig(name).isEmpty()) {
            properties.put(name + ".apiUrl", apiUrl);
            properties.put(name + ".email", email);
            properties.put(name + ".password", password);
            writeConfig(properties);
            System.out.println("added " + name);
            return 1;
        } else {
            System.err.println("config " + name + " already exists");
            return -1;
        }
    }

    public void removeConfig(String name) {
        Properties properties = loadProperties();
        AtomicBoolean removed = new AtomicBoolean(false);
        properties.keySet().forEach(o -> {
            if (o.toString().startsWith(name + ".")) {
                properties.remove(o.toString());
                removed.set(true);
            }
        });
        writeConfig(properties);
        if (removed.get()) {
            System.out.println("removed " + name);
        }
    }

    public Set<String> listConfig() {
        Properties properties = loadProperties();
        return properties.keySet().stream()
                .map(o -> ((String)o).substring(0, ((String)o).indexOf('.')))
                .collect(Collectors.toSet());
    }

    private static void writeConfig(Properties properties) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(getFile())) {
            properties.store(fileOutputStream, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, String> loadConfig(String prefix) {
        Properties properties = loadProperties();
        return properties.keySet().stream()
                .filter(o -> ((String)o).startsWith(prefix))
                .map(o -> (String)o)
                .collect(Collectors.toMap(s -> s.substring(s.indexOf('.') + 1), properties::getProperty));
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        File config = getFile();
        if (config.exists()) {
            try (FileInputStream is = new FileInputStream(config)) {
                properties.load(is);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            File homeDir = config.getParentFile();
            if (!homeDir.mkdirs()) {
                throw new IllegalStateException("could not create " + homeDir);
            }
        }
        return properties;
    }

    private static File getFile() {
        String home = System.getenv("HOME");
        if (home == null) {
            home = System.getenv("USERPROFILE");
        }
        if (home == null) {
            throw new IllegalStateException("could not find the user home directory");
        }
        File config = new File(home, ".collar/tools.properties");
        return config;
    }
}
