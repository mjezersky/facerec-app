
package facerec;


public class FacerecConfig {
    public static String WORKER_GROUP_NAME = "default";
    public static String OPENCV_BIN_PATH = "D:\\Programs\\opencv\\build\\java\\x64";
    public static String RABBIT_MQ_SERVER_IP = "localhost";
    public static int RABBIT_MQ_SERVER_PORT = 5672;
    public static String FACE_DB_FILENAME = "faces.csv";
    public static String UNKNOWN_NAME_PREFIX = "Unknown_";
    public static double FRAME_SKIP = 1;
    public static double RECTANGLE_MATCH_THRESHOLD = 0.25;
    public static long DEFAULT_DISCOVER_TIMEOUT = 4000;
    public static boolean UNKNOWN_CLUSTERING_ENABLED = true;
}
