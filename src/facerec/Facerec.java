package facerec;

import java.lang.reflect.Field;
import java.util.concurrent.Semaphore;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.opencv.core.Core;


public class Facerec extends Application {

    public static Stage currentStage = null;
    public static boolean GUI_ENABLED = true;
    public static CommandLine cmd = null;
    private static final Semaphore cliSem = new Semaphore(0);
    
    @Override
    public void start(Stage stage) throws Exception {
        Facerec.currentStage = stage;
        Parent root = FXMLLoader.load(getClass().getResource("Scene.fxml"));
        
        Scene scene = new Scene(root);
        //hiddenScene = new Scene(root);
        
        stage.setScene(scene);
        stage.setTitle("Face Recognition");
        stage.show();
    }
    
    private static void loadOpenCV() {

        
        System.setProperty("java.library.path", FacerecConfig.OPENCV_BIN_PATH);
 
        //set sys_paths to null
        try {
            final Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
            sysPathsField.setAccessible(true);
            sysPathsField.set(null, null);
        }
        catch (Exception ex) { }
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        }
        catch (Exception ex) {
            System.err.println("Error: failed to load openCV library.");
            System.exit(-1);
        }
    }
    
    public static void cliWait() {
        try {
            cliSem.acquire();
        } catch (InterruptedException ex) {
            System.err.println("Processing interrupted.");
        }
    }
    
    public static void cliFinish() {
        System.err.println("Calling finish.");
        cliSem.release();
    }
    
    private static int parseArgs(String[] args) {
        
        if (args.length < 1) { return 0; }
        
        if ( !(args[0].equals("-c") || args[0].equals("-h") || args[0].equals("--help")) ) {
            System.err.println("Error: invalid arguments.");
            return -1;
        }
        
        Facerec.GUI_ENABLED = false;
        
        CommandLineParser parser = new DefaultParser();
        
        Options options = new Options();
        
        Option cliopt = Option.builder("c")
                        .desc("use command line interface instead of GUI" )
                        .build();
        
        Option sourcefiles = Option.builder("s")
                        .longOpt("source")
                        .hasArgs()
                        .desc("video file(s) to process")
                        .build();
        
        Option outdir = Option.builder("o")
                        .longOpt("out-dir")
                        .hasArg()
                        .desc("directory for output files")
                        .build();
        
        Option rawonly = Option.builder("r")
                        .longOpt("raw-only")
                        .hasArg()
                        .desc("do not process raw output files")
                        .build();
        
        Option infiles = Option.builder("i")
                        .longOpt("input-files")
                        .hasArgs()
                        .desc("raw file(s) to process")
                        .build();
        
        Option capdev = Option.builder("d")
                        .longOpt("capture-device")
                        .hasArg()
                        .desc("use capture device instead of file")
                        .build();
        
        Option timeout = Option.builder("t")
                        .longOpt("discover-timeout")
                        .hasArg()
                        .desc("timeout for worker discovery")
                        .build();
        
        
        options.addOption(cliopt);
        options.addOption(sourcefiles);
        options.addOption(capdev);
        options.addOption(outdir);
        options.addOption(rawonly);
        options.addOption(infiles);
        options.addOption(timeout);
        
        try {
            Facerec.cmd = parser.parse(options, args);
        } catch (ParseException ex) {
            System.err.println("Error: invalid arguments.");
            return -1;
        }
        
        return 1;
    }
    
    private static long getDiscoverTimeout() {
        if (cmd.hasOption("t")) {
            try {
                return Long.parseLong(cmd.getOptionValue("t"));
            }
            catch (NumberFormatException ex) {
                return FacerecConfig.DEFAULT_DISCOVER_TIMEOUT;
            }
        }
        else {
            return FacerecConfig.DEFAULT_DISCOVER_TIMEOUT;
        }
    }
    
    private static void runcli() {
        Controller c = new Controller();
        c.initialize(null, null);
        c.discoverCLI();
        try {
            Thread.sleep(getDiscoverTimeout());
        } catch (InterruptedException ex) { }
        
        if (c.getWorkerPool().getWorkers().isEmpty()) {
            System.err.println("No workers.");
            return;
        }
        
        
        for (String filename : cmd.getOptionValues("s")) {
            c.setSourceCLI(filename);
            System.err.println("CLI wait start.");
            cliWait();
            System.err.println("CLI wait end.");
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        loadOpenCV();
        
        int argres = parseArgs(args);
        if (argres == 0) {
            launch(args);
        }
        else if (argres == 1 ) {
            runcli();
            System.exit(0);
        }
        else {
            System.exit(-1);
        }
        
    }
    
    @Override
    public void stop(){
        MQLink link = MQLink.getLink();
        if (link == null) { return; }
        System.out.println("Closing");
        link.close();
    }
    
}
