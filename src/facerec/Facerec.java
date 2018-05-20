package facerec;

import facerec.dbwindow.DBWindowController;
import facerec.flow.Flow;
import facerec.flow.FlowRecorder;
import facerec.flow.ListElementFlow;
import facerec.flow.Occurrence;
import facerec.result.RawFileReader;
import facerec.result.SearchRewriter;
import facerec.videowindow.VideoStatistics;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.opencv.core.Core;

/**
 * Main class of the program, handles command line arguments and GUI/CLI startup.
 * @author Matous Jezersky
 */
public class Facerec extends Application {

    public static Stage currentStage = null;
    public static boolean GUI_ENABLED = true;
    public static CommandLine cmd = null;
    private static final Semaphore cliSem = new Semaphore(0);
    
    /**
     * Startup method for JavaFX.
     * @param stage initial stage
     */
    @Override
    public void start(Stage stage) throws Exception {
        Facerec.currentStage = stage;
        Parent root = FXMLLoader.load(getClass().getResource("Scene.fxml"));
        
        Scene scene = new Scene(root);
        //hiddenScene = new Scene(root);
        
        stage.setScene(scene);
        stage.setTitle("Face Recognition");
        stage.getIcons().add(new Image(Facerec.class.getResourceAsStream("icon.png")));
        stage.show();
    }
    
    /**
     * Reports an error either via GUI or CLI, depending on which one is currently being used.
     * @param text error message to display
     */
    public static void error(String text) {
        if ( GUI_ENABLED && (Controller.getCurrentController() != null) ) {
            Controller.getCurrentController().displayAlert(text, Alert.AlertType.ERROR);
        }
        else {
            System.err.println("Error: "+text);
        }
    }
    
    /**
     * Reports info either via GUI or CLI, depending on which one is currently being used.
     * @param text info message to display
     */
    public static void info(String text) {
        if ( GUI_ENABLED && (Controller.getCurrentController() != null) ) {
            Controller.getCurrentController().printStatus(text+"\n");
        }
        else {
            System.out.println(text);
        }
    }
    
    /**
     * Reports a warning either via GUI or CLI, depending on which one is currently being used.
     * @param text warning message to display
     */
    public static void warning(String text) {
        if ( GUI_ENABLED && (Controller.getCurrentController() != null) ) {
            Controller.getCurrentController().displayAlert(text, Alert.AlertType.WARNING);
        }
        else {
            System.err.println("Warning: "+text);
        }
    }
    
    // method to load openCV library in various ways (differs between platforms)
    private static void loadOpenCV() {
        
        // try multiple variants
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            return;
        }
        catch ( UnsatisfiedLinkError | Exception ex) { }
        try {
            System.loadLibrary(FacerecConfig.OPENCV_LIBRARY_NAME);
            return;
        }
        catch ( UnsatisfiedLinkError | Exception ex) { }

        // try manual load
        try {
            System.load(new File(FacerecConfig.OPENCV_BIN_PATH+"/"+FacerecConfig.OPENCV_LIBRARY_NAME).getAbsolutePath());
            return;
        }
        catch ( UnsatisfiedLinkError | Exception ex) { }
        
        // another attempt
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
            return;
        }
        catch ( UnsatisfiedLinkError | Exception ex) { }
        try {
            System.loadLibrary(FacerecConfig.OPENCV_LIBRARY_NAME);
        }
        catch ( UnsatisfiedLinkError | Exception ex) {
            System.err.println("Error: failed to load openCV library.");
            System.exit(-1);
        }
    }
    
    // Wait until cliFinish is called
    private static void cliWait() {
        try {
            cliSem.acquire();
        } catch (InterruptedException ex) {
            System.err.println("Processing interrupted.");
        }
    }
    
    /**
     * Reports finish and breaks CLI from waiting.
     */
    public static void cliFinish() {
        System.err.println("Calling finish.");
        cliSem.release();
    }
    
    // command line argument parsing
    private static int parseArgs(String[] args) {
        
        if (args.length < 1) { return 0; }
        
        
        Facerec.GUI_ENABLED = false;
        
        CommandLineParser parser = new DefaultParser();
        
        Options options = new Options();
        
        Option processopt = Option.builder("p")
                        .desc("process video file(s) specified by -i parameter" )
                        .build();
        
        Option analyzeopt = Option.builder("a")
                        .desc("analyze processed file(s) specified by -i parameter" )
                        .build();
        
        Option dbopt = Option.builder("d")
                        .longOpt("dbadd")
                        .hasArg()
                        .desc("add files specified by -i parameter to database, under the name set by this parameter" )
                        .build();
        
        Option printdbopt = Option.builder("l")
                        .longOpt("list-db")
                        .desc("list names in face database" )
                        .build();
        
        Option searchopt = Option.builder("s")
                        .longOpt("search")
                        .hasArg()
                        .desc("advanced search for a single person, specified by name")
                        .build();
        
        Option inputfilesopt = Option.builder("i")
                        .longOpt("input")
                        .hasArgs()
                        .desc("input file(s) to process")
                        .build();
        
        Option outdiropt = Option.builder("o")
                        .longOpt("out-dir")
                        .hasArg()
                        .desc("directory for output files")
                        .build();
        
        Option exportopt = Option.builder("e")
                        .longOpt("export-passes")
                        .hasArg()
                        .desc("export passes in CSV format into file specified by argument")
                        .build();
        
        Option facedbopt = Option.builder("f")
                        .longOpt("faces")
                        .hasArg()
                        .desc("face database file (default is faces.csv)")
                        .build();
        
        Option timeoutopt = Option.builder("t")
                        .longOpt("discover-timeout")
                        .hasArg()
                        .desc("timeout for worker discovery")
                        .build();
        
        
        options.addOption(processopt);
        options.addOption(analyzeopt);
        options.addOption(inputfilesopt);
        options.addOption(exportopt);
        options.addOption(dbopt);
        options.addOption(printdbopt);
        options.addOption(searchopt);
        options.addOption(outdiropt);
        options.addOption(facedbopt);
        options.addOption(timeoutopt);
        
        try {
            Facerec.cmd = parser.parse(options, args);
        } catch (ParseException ex) {
            System.err.println("Error: invalid arguments.");
            return -1;
        }
        
        return 1;
    }
    
    // returns timeout for CLI discover in ms
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
    
    // pads string up to toLen with spaces suffix
    private static String padString(String str, int toLen) {
        if (str == null) { return null; }
        String newStr = str;
        for (int i=str.length(); i<=toLen; i++) {
            newStr += " ";
        }
        return newStr;
    }
    
    // returns String representation of Flow
    private static String getFlowString(Flow f) {
        Occurrence oc = f.mostFrequentOccurrence();
        int textCellSize = 18;
        
        String time = padString(ListElementFlow.getTime(f.firstSecond) + "-" + ListElementFlow.getTime(f.lastSecond) + " ", textCellSize);
        String frames = padString("(" + Long.toString((long) f.firstFrame) + "-" + Long.toString((long) f.lastFrame) +") ", textCellSize);
        String name = padString(oc.name + " ", textCellSize);
        String confidence = padString(String.format("%.2f", oc.bestConfidence) + " ", textCellSize);
        String bestFrame = Long.toString((long) oc.bestFrame);
        
        return time+frames+name+confidence+bestFrame;
    }
    
    // -a parameter method
    private static void analyze(Controller c, String searchName) {
        for (String filename : cmd.getOptionValues("i")) {
            try {
                FlowRecorder fr;
                
                // set flow recorder depending on filter
                if (searchName != null) {
                    fr = RawFileReader.openRawAsFlow( filename, searchName, c.getDBBridge().getFaceDB() );
                }
                else {
                    fr = RawFileReader.openRawAsFlow(filename);
                }
                VideoStatistics vs = new VideoStatistics();

                System.out.println(filename+":");
                System.out.println("Time               (Frames)           Name               Confidence         Best frame");
                
                // list all flows
                for (Flow f : fr.getFlows()) {
                    Occurrence oc = f.mostFrequentOccurrence();
                    vs.feed(oc);
                    
                    // if filter is set, print only when names match
                    if (searchName != null) {
                        // change negative distance to positive
                        oc.bestConfidence = -oc.bestConfidence;
                        
                        if (oc.name.equals(searchName) && oc.bestConfidence <= FacerecConfig.SEARCH_DIST_THRESHOLD_HARD) {
                            // convert distance to confidence
                            oc.bestConfidence = SearchRewriter.distToConf(oc.bestConfidence);
                            System.out.println(getFlowString(f));
                        }
                    }
                    else {
                        System.out.println(getFlowString(f));
                    }

                }
                // process collected statistics and print them
                vs.process();
                System.out.println(vs.toString());
                
                // if export option is set, write into file
                if (cmd.hasOption("e")) {
                    try { fr.writeToFile(cmd.getOptionValue("e")); }
                    catch(IOException ex) { error("Error: cannot write to file "+cmd.getOptionValue("e"));  }
                }


            } catch (IOException ex) {
                error("Error: cannot read file "+filename);
            }
        }
    }
    
    // -p parameter method
    private static void processVideo(Controller c) {
        // initiate worker discovery
        c.discoverCLI();
        
        // wait a while
        try {
            Thread.sleep(getDiscoverTimeout());
        } catch (InterruptedException ex) { }

        if (c.getWorkerPool().getWorkers().isEmpty()) {
            System.err.println("No workers.");
            return;
        }

        // process input files
        for (String filename : cmd.getOptionValues("i")) {
            File f = new File(filename);
            c.setSourceCLI(f.getAbsolutePath());
            System.err.println("CLI wait start.");
            //Perf p = new Perf(); // DEBUG
            //p.start();
            
            // wait until dispatcher and controller report end of processing
            cliWait();
            System.err.println("CLI wait end.");
            //System.err.println(p.stop());
        }
    }
    
    // -d parameter method
    private static void dbAdd(Controller c) {
        // initiate discovery request
        c.discoverCLI();
        
        // sleep in parts and check if found at least one worker, once one is found, proceed
        for (int i = 0; i<=getDiscoverTimeout(); i+=200) {
            try { Thread.sleep(200);}
            catch (InterruptedException ex) { }
            if (!c.getWorkerPool().getWorkers().isEmpty()) { break; }
        }
        if (c.getWorkerPool().getWorkers().isEmpty()) {
            System.err.println("No workers.");
            return;
        }
        
        // initialize db for writing
        DBWindowController dbw = new DBWindowController();
        dbw.initialize(null, null);
        c.getDBBridge().registerDBWController(dbw);
        
        // add all files into processing list
        for (String filename : cmd.getOptionValues("i")) {
            File f = new File(filename);
            dbw.listAdd(cmd.getOptionValue("d"), f.getAbsolutePath());
        }
        
        System.out.println("Processing images...");
        
        // process list and wait until DBWindowController reports end
        dbw.processImages();
        cliWait();
        
    }
    
    // -l parameter method
    private static void printDB(Controller c) {
        ArrayList<String[]> faces = c.getDBBridge().getFaceDB().getFaces();
        int ind = 1;
        for (String[] face : faces ) {
            System.out.println(Integer.toString(ind) + ": " + face[0]);
            ind++;
        }
    }
        
    // main CLI method
    private static void runcli() {
        // set variables
        try {
            if (cmd.hasOption("o")) { FacerecConfig.OUT_FILES_DIR = cmd.getOptionValue("o"); }
            if (cmd.hasOption("f")) { FacerecConfig.FACE_DB_FILENAME = cmd.getOptionValue("f"); }
            if (cmd.hasOption("t")) { FacerecConfig.DEFAULT_DISCOVER_TIMEOUT = Integer.parseInt(cmd.getOptionValue("t")); }
        }
        catch (NumberFormatException ex) {
            error("Invalid arguments.");
            return;
        }
        
        // initialize main controller
        Controller c = new Controller();
        c.initialize(null, null);
        
        if (cmd.hasOption("p")) {
            processVideo(c);
        }
        else if (cmd.hasOption("a")) {
            analyze(c, null);
        }
        else if (cmd.hasOption("d")) {
            dbAdd(c);
        }
        else if (cmd.hasOption("l")) {
            printDB(c);
        }
        else if (cmd.hasOption("s")) {
            FacerecConfig.UNKNOWN_CLUSTERING_ENABLED = false;
            analyze(c, cmd.getOptionValue("s"));
        }
    }

    /**
     * Main method of the program.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        
        FacerecConfig.loadConfigFile();
        
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
    
    
    /**
     * Closes connections before stopping GUI by closing the window.
     */
    @Override
    public void stop(){
        MQLink link = MQLink.getLink();
        if (link == null) { return; }
        link.close();
    }
    
}
