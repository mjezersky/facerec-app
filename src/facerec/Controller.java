package facerec;

import facerec.dbwindow.DBBridge;
import facerec.dbwindow.DBWindowController;
import facerec.flow.FlowRecorder;
import facerec.result.RawFileReader;
import facerec.videowindow.VideoWindowController;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Main controller class for GUI and CLI operations.
 * @author Matous Jezersky
 */
public class Controller implements Initializable {
    
    @FXML private TextArea statusText;
    @FXML private Label imageStatus;
    @FXML private TextField ipAddrIn;
    @FXML private TextField portIn;
    @FXML private TextField frameIn;
    @FXML private ImageView image;
    @FXML private MediaView video;
    @FXML private ImageView processedImage;
    @FXML private SwingNode swingNode;
    @FXML private Button cancelButton;
    @FXML private ListView workerView;
    @FXML private Pane imgContainer;
    @FXML private MenuBar menuBar;
    @FXML private ProgressBar progressBar;
    
    private Rectangle displayRect = null;    
    private VideoController vc = null;
    private MediaPlayer player;
    boolean processing = false;
    boolean displayRectangles = true;
    private MediaView hiddenVideo;
    private WorkerPool workerPool;
    private Dispatcher dispatcher;
    private MQLink mqlink;
    private DBBridge dbbridge;
    private String currDataFile;
    private int lastPrintedProgress;
    
    private Stage dbwStage;
    private Stage vwStage;
    
    private FlowRecorder vwFlowRecorder = null;
    private VideoController vwVideoController = null;
    
    private static Controller currentController = null;
    
    /**
     * Returns the current (last initialized) controller.
     * @return current controller
     */
    public static Controller getCurrentController() { return currentController; }
    
    /**
     * Returns worker pool assigned to the controller.
     * @return assigned worker pool
     */
    public WorkerPool getWorkerPool() { return workerPool; }
    
    /**
     * Returns dispatcher assigned to the controller.
     * @return assigned dispatcher
     */
    public Dispatcher getDispatcher() { return dispatcher; }
    
    /**
     * Returns FlowRecorder for video window assigned to the controller.
     * @return assigned worker pool
     */
    public FlowRecorder getVWFlowRecorder() { return vwFlowRecorder; }
    
    /**
     * Returns VideoController for video window assigned to the controller.
     * @return assigned video controller
     */
    public VideoController getVWVideoController() { return vwVideoController; }
    
    /**
     * Returns stage of database window if active (or null).
     * @return stage for database window
     */
    public Stage getDBWStage() { return dbwStage; }
    
    /**
     * Returns stage of video window if active (or null).
     * @return stage for video window
     */
    public Stage getVWStage() { return vwStage; }
    
    /**
     * Returns DBBridge for database window assigned to the controller.
     * @return assigned DBBridge
     */
    public DBBridge getDBBridge() { return dbbridge; }
    
    /**
     * Returns current (last used) data file (or null).
     * @return current data file
     */
    public String getCurrDataFile() { return currDataFile; }
    
    @FXML
    private void updateSel(MouseEvent event) {
        // to be removed   
    }
    
    /**
     * Displays an alert window.
     * @param text text for the window content
     * @param type type of alert to display
     */
    public void displayAlert(String text, AlertType type) {
        Alert alert = new Alert(type);
        if (type.equals(AlertType.ERROR)) {
            alert.setTitle("Error");
        }
        else {
            alert.setTitle("Warning");
        }
        alert.setHeaderText(null);
        alert.setContentText(text);

        alert.showAndWait();
    }
    
    /**
     * Displays text in the Info section of main window.
     * @param str text to display
     */
    public void printStatus(String str) {
        try {
            statusText.setEditable(true);
            statusText.appendText(str);
            statusText.setEditable(false);
        }
        catch (Exception ex) {} // can happen when write buffer is overwhelmed, ignore it
    }
    
    /**
     * Stops processing of current video file.
     * @param event action event for button, can be set as null
     */
    @FXML
    public void cancelProcessing(ActionEvent event) {
        dispatcher.stop();
        cancelButton.setDisable(true);
    }
    
    @FXML
    private void updateLabel(KeyEvent event) {
        //vc.getLink().setTrainLabel( ((TextField) event.getSource()).getText() );
    }
    
    @FXML
    private void updateDB(ActionEvent event) {
        printStatus("DB update\n");
        try {
            mqlink.broadcast("broadcast", "1"+dbbridge.getFaceDB().toString());
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * Broadcast face DB to all available workers.
     * @throws IOException
     */
    public void broadcastDB() throws IOException {
        mqlink.broadcast("broadcast-" + FacerecConfig.WORKER_GROUP_NAME, "1"+dbbridge.getFaceDB().toString() );
    }
    
    @FXML
    private void discover(ActionEvent event) {
        workerPool.clear();
        try {
            mqlink.connect();
            mqlink.declareExchange("broadcast-" + FacerecConfig.WORKER_GROUP_NAME);
            mqlink.declareQueue("feedback-" + FacerecConfig.WORKER_GROUP_NAME);
            
            mqlink.registerDispatcher(dispatcher);
            mqlink.broadcast("broadcast-" + FacerecConfig.WORKER_GROUP_NAME, "0DISCOVER");
            broadcastDB();
        }
        catch (Exception ex) {
            Facerec.error("Discover failed, please check your RabbitMQ connectivity or credentials.\n\nError type: "+ex.getClass().getCanonicalName());
        }
    
    }
    
    
    // method for Export Passes button press
    @FXML
    private void exportPasses(ActionEvent event) {
         // choose data file
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*"));
        fileChooser.setTitle("Choose source data file");
        File dataFile = fileChooser.showOpenDialog(Facerec.currentStage);
        
        if (dataFile == null) { return; }
        
        // set output file
        try {
            fileChooser = new FileChooser();
            fileChooser.setTitle("Export to");
            if (dataFile.getParent() != null) {
                fileChooser.setInitialDirectory(new File(dataFile.getParent()));
            }
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*"));
            fileChooser.setInitialFileName("passes.csv");
            File outfile = fileChooser.showSaveDialog(Facerec.currentStage);
            if (outfile == null) {
                return;
            }
            // read, convert and write data
            RawFileReader.processRawFile(dataFile.getAbsolutePath(), outfile.getAbsolutePath());
        }
        catch (IOException ex) {
            Facerec.error("Cannot export file - read, write or conversion error.");
        }
    }
        
    /**
     * Updates progress bar or text status of processing.
     * @param value current progress in range 0.0-1.0
     */
    public void updateProgressBar(double value) {
        // update progress bar in GUI
        if (Facerec.GUI_ENABLED) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    if (!dispatcher.hasFinished()) {
                        cancelButton.setDisable(false);
                    }
                    progressBar.setProgress(value);
                }
            });
        }
        // write every fifth percent in CLI
        else {
            int intProgress = ((int) (value*100));
            if ( intProgress%5 == 0 ) {
                if (intProgress > lastPrintedProgress) {
                    System.out.println(intProgress);
                }
                lastPrintedProgress = intProgress;
            }
        }
    }
    
    // simple method to find the video file which corresponds to the data file, if possible
    private File correspondingVideoFile(String dataFilePath) {
        if (dataFilePath.length()<5) { return null; }
        
        // remove .csv suffix
        String vfname = dataFilePath.substring(0, dataFilePath.length()-4);

        // check if exists, then return it, else return null
        File result = new File(vfname);
        if (result.exists()) {
            return result;
        }
        else {
            return null;
        }
    }
    
    // method for source selection and processing initialization
    @FXML
    private void selectSource(ActionEvent event) {
        lastPrintedProgress = 0;
        
        if (workerPool.getWorkers().isEmpty()) {
            Facerec.error("No workers available. Please use discover first.");
            return;
        }
        
        // create a disposable VideoController to check frames
        vc = new VideoController(this);
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Specify input source");
        File infile = fileChooser.showOpenDialog(Facerec.currentStage);
        
        if (infile == null) { return; }
        
        String fname = infile.getAbsolutePath();
        
        // load video file
        vc.setSource(fname);
        printStatus("Source set as \""+fname+"\", total frames: "+String.valueOf(vc.getTotalFrames())+"\n");
        
        // initialize and start processing
        dispatcher.distributeWork(fname);
    }
    
    /**
     * CLI method to initialize and start processing of a video file.
     * @param filename source video file
     */
    public void setSourceCLI(String filename) {
        lastPrintedProgress = 0;
        vc = new VideoController(this); 
        vc.setSource(filename);
        dispatcher.distributeWork(filename);
    }
    
    /**
     * CLI method wrapper for worker discovery.
     */
    public void discoverCLI() {
        discover(null);
    }
    
    /**
     * Method to call when reporting end of processing.
     */
    public void finish() {
        Facerec.info("Finished.");
        if (Facerec.GUI_ENABLED) {
            cancelButton.setDisable(true);
        }
        else {
            Facerec.cliFinish();
        }
    }
    
    /**
     * Method to open a processed file and the corresponding video file in a new window
     * @param evt button event, can be set as null
     */
    @FXML
    public void openVideoWindow(ActionEvent evt) {
        
        // choose data file
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose data file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*"));
        File dataFile = fileChooser.showOpenDialog(Facerec.currentStage);
        
        if (dataFile == null) { return; }
        currDataFile = dataFile.getAbsolutePath();
        
        // choose optional video file
        File videoFile = correspondingVideoFile(dataFile.getAbsolutePath());
        if (videoFile == null) {
            fileChooser = new FileChooser();
            if (dataFile.getParent() != null) {
                fileChooser.setInitialDirectory(new File(dataFile.getParent()));
            }
            fileChooser.setTitle("Choose corresponding video file (optional)");
            videoFile = fileChooser.showOpenDialog(Facerec.currentStage);
        }
        
        // read the files
        try {
            vwFlowRecorder = RawFileReader.openRawAsFlow(dataFile.getAbsolutePath());
            vwVideoController = new VideoController();
            if (videoFile != null) {
                vwVideoController.setSource(videoFile.getAbsolutePath());
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        
        // open window
        try {
            Parent root = FXMLLoader.load(VideoWindowController.class.getResource("VideoWindow.fxml"));
            vwStage = new Stage();
            vwStage.setTitle("Video "+dataFile.getName());
            vwStage.getIcons().add(new Image(Facerec.class.getResourceAsStream("icon.png")));
            vwStage.setScene(new Scene(root));
            vwStage.initModality(Modality.WINDOW_MODAL);
            vwStage.initOwner( statusText.getScene().getWindow() );
            vwStage.show();

        } catch (IOException e) {e.printStackTrace();}
    }
    
    /**
     * Opens a window for editing and viewing face DB.
     * @param evt button event, can be null
     */
    @FXML
    public void openDBWindow(ActionEvent evt) {

        // open window
        try {
            Parent root = FXMLLoader.load(DBWindowController.class.getResource("dbwindow.fxml"));
            dbwStage = new Stage();
            dbwStage.setTitle("Face Database");
            dbwStage.getIcons().add(new Image(Facerec.class.getResourceAsStream("icon.png")));
            dbwStage.setScene(new Scene(root));
            dbwStage.show();

        } catch (IOException e) {e.printStackTrace();}
    }
    
    
    /**
     * Initializes the controller.
     * @param url JavaFX argument, can be null if running without GUI
     * @param rb JavaFX argument, can be null if running without GUI
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Controller.currentController = this;
        lastPrintedProgress = 0;
        
        if (Facerec.GUI_ENABLED) {
            hiddenVideo = new MediaView();
            StackPane.setAlignment(processedImage, Pos.TOP_LEFT);
        }
        workerPool = new WorkerPool(workerView);
        //workerPool.addWorker("localhost", 9000);
        MQLink.makeLink();
        mqlink = MQLink.getLink();;
        dbbridge = new DBBridge(mqlink);
        dispatcher = new Dispatcher(workerPool, mqlink, dbbridge);
        
    }
    
    
}
