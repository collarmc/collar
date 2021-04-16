package team.catgirl.collar.client;

import java.io.File;
import java.io.IOException;

public final class HomeDirectory {

    private final File collarHome;

    private HomeDirectory(File collarHome) {
        this.collarHome = collarHome;
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
     * @return directory containing collar profile information
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
        return new File(collarHome, "debug");
    }

    private File createDirectory(String dht) throws IOException {
        File dir = new File(collarHome, dht);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("could not make directory " + dir.getAbsolutePath());
        }
        return dir;
    }

    /**
     * @param mcHome of the minecraft client
     * @param hostName of the collar server
     * @return collar home directory
     * @throws IOException if directories could not be created
     */
    public static HomeDirectory from(File mcHome, String hostName) throws IOException {
        File collar = new File(mcHome, "collar/" + hostName);
        if (!collar.exists() && !collar.mkdirs()) {
            throw new IOException("could not make directory " + collar.getAbsolutePath());
        }
        return new HomeDirectory(collar);
    }
}
