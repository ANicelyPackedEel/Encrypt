import java.io.InputStream;
import java.net.URL;

/**
 * Utility class which manages the access to this project's assets.
 * Helps keeping the assets files structure organized.
 */
public class MFXDemoResourcesLoader {

    private MFXDemoResourcesLoader() {
    }

    public static URL loadURL(String path) {
        return MFXDemoResourcesLoader.class.getResource(path);
    }

    public static String load(String path) {
        return loadURL(path).toString();
    }

    public static InputStream loadStream(String name) {
        return MFXDemoResourcesLoader.class.getResourceAsStream(name);
    }

}