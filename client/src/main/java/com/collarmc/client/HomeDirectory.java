package com.collarmc.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.StandardOpenOption;

/**
 * Collar home directory and well known locations
 */
public final class HomeDirectory {

    private static final Logger LOGGER = LogManager.getLogger(HomeDirectory.class.getName());

    private final File mcHome;
    private final File collarHome;
    private final DirectoryLock lock;

    private HomeDirectory(File mcHome, File collarHome) throws IOException {
        if (!collarHome.exists() && !collarHome.mkdirs()) {
            throw new IOException("could not make directory " + collarHome.getAbsolutePath());
        }
        this.mcHome = mcHome;
        this.collarHome = collarHome;
        this.lock = new DirectoryLock(this);
    }

    /**
     * @return directory containing all security info, such as public/private keys
     * @throws IOException if directories could not be created
     */
    public File security() throws IOException {
        return createDirectory("security");
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

    /**
     * @return lock
     */
    DirectoryLock getLock() {
        return lock;
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

    /**
     * Locks the home directory when the client is running so multiple processes don't stomp the Signal state
     */
    static final class DirectoryLock {

        private static final Object LOCK = new Object();

        private FileChannel lockFile;
        private FileLock lock;

        private final HomeDirectory homeDirectory;

        public DirectoryLock(HomeDirectory homeDirectory) {
            this.homeDirectory = homeDirectory;
        }

        /**
         * Locks the home directory
         * @return locked
         */
        public boolean tryLock() {
            synchronized (LOCK) {
                try {
                    lockFile = FileChannel.open(
                            new File(homeDirectory.collarHome, "collar.lock").toPath(),
                            StandardOpenOption.CREATE,
                            StandardOpenOption.WRITE);
                    lock = lockFile.tryLock();
                    return true;
                } catch (IOException | OverlappingFileLockException e) {
                    LOGGER.warn( "Could not get lock on home directory", e);
                    return false;
                }
            }
        }

        /**
         * Unlocks the home directory
         */
        @SuppressFBWarnings(value = "DE_MIGHT_IGNORE", justification = "we don't care")
        public void unlock() {
            synchronized (LOCK) {
                try {
                    if (lock != null) lock.release();
                } catch (IOException e) {
                    LOGGER.warn( "Could not close home directory lock", e);
                }
                try {
                    if (lockFile != null) lockFile.close();
                } catch (IOException e) {
                    LOGGER.warn( "Could not close home directory lock channel", e);
                }
                lockFile = null;
                lock = null;
            }
        }
    }
}
