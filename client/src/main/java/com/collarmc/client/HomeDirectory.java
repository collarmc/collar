package com.collarmc.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Collar home directory and well known locations
 */
public final class HomeDirectory {

    private static final Logger LOGGER = LogManager.getLogger(HomeDirectory.class.getName());

    private final File mcHome;
    private final File collarHome;

    private HomeDirectory(File mcHome, File collarHome) throws IOException {
        if (!collarHome.exists() && !collarHome.mkdirs()) {
            throw new IOException("could not make directory " + collarHome.getAbsolutePath());
        }
        this.mcHome = mcHome;
        this.collarHome = collarHome;
    }

    /**
     * @return directory containing collar profile information
     * @throws IOException if directories could not be created
     */
    public File profile() throws IOException {
        return createDirectory("profile");
    }

    /**
     * @return directory containing collar dht state
     * @throws IOException if directories could not be created
     */
    public File dhtState() throws IOException {
        return createDirectory("dht");
    }

    /**
     * If this file exists, it enables development features that are used for debugging
     * @return the debug file
     */
    public File debugFile() {
        return new File(mcHome, "collar/debug");
    }

    private File createDirectory(String path) throws IOException {
        File dir = new File(collarHome, path);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("could not make directory " + dir.getAbsolutePath());
        }
        return dir;
    }

    @Override
    public String toString() {
        return collarHome.getAbsolutePath();
    }

    /**
     * @param mcHome of the minecraft client
     * @param hostName of the collar server
     * @return collar home directory
     * @throws IOException if directories could not be created
     */
    public static HomeDirectory from(File mcHome, String hostName) throws IOException {
        return new HomeDirectory(mcHome, new File(mcHome, "collar/" + hostName));
    }
}
