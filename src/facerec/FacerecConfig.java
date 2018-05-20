
package facerec;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Class to store the program configuration in.
 * @author Matous Jezersky
 */
public class FacerecConfig {
    
    public static String CONFIG_FILE_NAME = "facerec.properties";
    
    public static String WORKER_GROUP_NAME = "default";
    public static String OPENCV_BIN_PATH = "D:\\Programs\\opencv\\build\\java\\x64";
    public static String OPENCV_LIBRARY_NAME = "opencv_java340";
    public static String RABBIT_MQ_SERVER_IP = "localhost";
    public static int RABBIT_MQ_SERVER_PORT = 5672;
    public static String RABBIT_MQ_USERNAME = "facerec";
    public static String RABBIT_MQ_PASSWORD = "facerec";
    public static int RABBIT_MQ_TIMEOUT = 8000;
    public static String FACE_DB_FILENAME = "faces.csv";
    public static String UNKNOWN_NAME_PREFIX = "Unknown_";
    public static double FRAME_SKIP = 1;
    public static double RECTANGLE_MATCH_THRESHOLD = 0.25;
    public static long DEFAULT_DISCOVER_TIMEOUT = 4000;
    public static boolean UNKNOWN_CLUSTERING_ENABLED = true;
    public static String FRAME_IMAGE_FORMAT = "JPG"; // JPG or BMP
    public static double UNKNOWN_DIST_THRESHOLD = 0.53;
    public static double SEARCH_DIST_THRESHOLD_SOFT = 0.59;
    public static double SEARCH_DIST_THRESHOLD_HARD = 0.51;
    public static boolean MERGE_NEARBY_FLOWS = true;
    public static double NEARBY_FLOWS_MAX_DIST = 200;
    public static String OUT_FILES_DIR = null;
    
    /**
     * Loads a config file specified by FacerecConfig.CONFIG_FILE_NAME and sets the corresponding fields.
     */
    public static void loadConfigFile() {
        Properties p = new Properties();
        FileInputStream fis;
        try {
            fis = new FileInputStream(CONFIG_FILE_NAME);
        }
        catch (FileNotFoundException ex) {
            Facerec.warning("Cannot read config file, setting all to default.");
            return;
        }
        
        try {
            p.load(fis);
        }
        catch (IOException ex) {
            Facerec.warning("Cannot read config file, setting all to default.");
            return;
        }
        
        WORKER_GROUP_NAME = p.getProperty("WORKER_GROUP_NAME", WORKER_GROUP_NAME);
        RABBIT_MQ_SERVER_IP = p.getProperty("RABBIT_MQ_SERVER_IP", RABBIT_MQ_SERVER_IP);
        RABBIT_MQ_USERNAME = p.getProperty("RABBIT_MQ_USERNAME", RABBIT_MQ_USERNAME);
        RABBIT_MQ_PASSWORD = p.getProperty("RABBIT_MQ_PASSWORD", RABBIT_MQ_PASSWORD);
        FACE_DB_FILENAME = p.getProperty("FACE_DB_FILENAME", FACE_DB_FILENAME);
        OPENCV_BIN_PATH = p.getProperty("OPENCV_BIN_PATH", OPENCV_BIN_PATH);
        OPENCV_LIBRARY_NAME = p.getProperty("OPENCV_LIBRARY_NAME", OPENCV_LIBRARY_NAME);
        OUT_FILES_DIR = p.getProperty("OUT_FILES_DIR", OUT_FILES_DIR);
        try { RABBIT_MQ_SERVER_PORT = Integer.parseInt(p.getProperty("RABBIT_MQ_SERVER_PORT", Integer.toString(RABBIT_MQ_SERVER_PORT))); }
        catch (NumberFormatException ex) {}
    }
    
}
