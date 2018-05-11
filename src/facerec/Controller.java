package facerec;

import facerec.dbwindow.DBBridge;
import facerec.dbwindow.DBWindowController;
import facerec.flow.FlowRecorder;
import facerec.result.RawFileReader;
import facerec.videowindow.VideoWindowController;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.TimeoutException;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
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
import javax.imageio.ImageIO;

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
    @FXML private Button processButton;
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
    
    private Stage dbwStage;
    private Stage vwStage;
    
    private FlowRecorder vwFlowRecorder = null;
    private VideoController vwVideoController = null;
    
    private static Controller currentController;
    
    
    public static Controller getCurrentController() { return currentController; }
    
    public WorkerPool getWorkerPool() { return workerPool; }
    
    public Dispatcher getDispatcher() { return dispatcher; }
    
    public FlowRecorder getVWFlowRecorder() { return vwFlowRecorder; }
    
    public VideoController getVWVideoController() { return vwVideoController; }
    
    public Stage getDBWStage() { return dbwStage; }
    
    public Stage getVWStage() { return vwStage; }
    
    public DBBridge getDBBridge() { return dbbridge; }
    
    @FXML
    private void addWorker(ActionEvent event) {
        printStatus("Not yet implemented\n");
    }
    
    @FXML
    private void editWorker(ActionEvent event) {
        printStatus("Editing worker\n");
        workerPool.editSelected(ipAddrIn.getText(), Integer.parseInt(portIn.getText()));
    }
    
    @FXML
    private void updateSel(MouseEvent event) {
        if (workerPool.isSelected()) {
            ipAddrIn.setText(workerPool.getSelectedIP());
            portIn.setText(String.valueOf(workerPool.getSelectedPort()));
            
        }
   
    }
    
    
    public void printStatus(String str) {
        statusText.setEditable(true);
        statusText.appendText(str);
        statusText.setEditable(false);
    }
    
    
    @FXML
    public void switchProcessing(ActionEvent event) {
        if (Dispatcher.isEnabled()) {
            printStatus("Processing stopped\n");
            Dispatcher.stop();
            processButton.setText("Start Processing");
        }
        else {
            printStatus("Processing started\n");
            Dispatcher.start();
            processButton.setText("Stop Processing");
        }
    }
    
    @FXML
    private void updateLabel(KeyEvent event) {
        vc.getLink().setTrainLabel( ((TextField) event.getSource()).getText() );
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
    
    
    @FXML
    private void setWebcamSource(ActionEvent event) {
        vc.setSource(0);
    }
    
    @FXML
    private void retrainClassifier(ActionEvent event) {
        printStatus("Retrain classifier request sent\n");
        printStatus("Response " + vc.getLink().sendCommand("RETRAIN") + "\n");
    }
    
    @FXML
    private void trainSwitch(ActionEvent event) {
        vc.setTrainingMode( ((CheckBox) event.getSource()).isSelected() );
    }
    
    @FXML
    private void rectSwitch(ActionEvent event) {
        displayRectangles = ( ((CheckBox) event.getSource()).isSelected() );
    }
    
    @FXML
    private void frameSeek(ActionEvent event) {
        double frame = Double.parseDouble(frameIn.getText());
        vc.seek(frame);
    }
    
    public void broadcastDB() throws IOException {
        mqlink.broadcast("broadcast-" + FacerecConfig.WORKER_GROUP_NAME, "1"+dbbridge.getFaceDB().toString() );
    }
    
    @FXML
    private void discover(ActionEvent event) {
        workerPool.clear();
        try {
            mqlink.connect(FacerecConfig.RABBIT_MQ_SERVER_IP, FacerecConfig.RABBIT_MQ_SERVER_PORT);
            mqlink.declareExchange("broadcast-" + FacerecConfig.WORKER_GROUP_NAME);
            mqlink.declareQueue("feedback-" + FacerecConfig.WORKER_GROUP_NAME);
            
            //mqlink.declareQueue("default");
            //mqlink.publish("default", "test");
            
            mqlink.registerDispatcher(dispatcher);
            mqlink.broadcast("broadcast-" + FacerecConfig.WORKER_GROUP_NAME, "0DISCOVER");
            broadcastDB();
        }
        catch (IOException | TimeoutException ex) {
            System.err.println("Controller discover error.");
            ex.printStackTrace();
        }
    
    }
    
    @FXML
    private void debugButton(ActionEvent event) {
        System.out.println(mqlink.isChannelOpen());
        System.out.println(mqlink.isConnectionOpen());
    }
    
    @FXML
    private void fileTestButton(ActionEvent event) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Specify input source");
            File outfile = fileChooser.showSaveDialog(Facerec.currentStage);
            if (outfile == null) {
                return;
            }
            RawFileReader.processRawFile("default.tmp", outfile.getAbsolutePath());
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
        
    
    public void updateProgressBar(double value) {
        if (Facerec.GUI_ENABLED) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    progressBar.setProgress(value);
                }
            });
        }
        else {
            // DEBUG!!!
            System.out.println(value);
        }
    }
    
    public void displayImage(Image img) {
        processedImage.setImage(img);
    }
    
    public void displayImage(Image img, String responseData) {
        processedImage.setImage(img);
        
        if (displayRect != null) {
            imgContainer.getChildren().remove(displayRect);
        }
        if (!displayRectangles) { return; }
        RectangleObject faceRect = RectangleObject.deserializeRect(responseData);
        if (faceRect != null) {
            
            
            // assuming the image is landscape-oriented, todo: add actual check for larger dim
            double scaleFactor = imgContainer.getHeight()/img.getHeight();
            faceRect.scale(scaleFactor);
            
            displayRect = new Rectangle(faceRect.left, faceRect.top, faceRect.right-faceRect.left, faceRect.bottom-faceRect.top);
            displayRect.getStyleClass().add("face-rectangle");
            imgContainer.getChildren().add(displayRect);

        }
    }
    
    private File correspondingVideoFile(String dataFilePath) {
        if (dataFilePath.length()<5) { return null; }
        
        String vfname = dataFilePath.substring(0, dataFilePath.length()-4);

        File result = new File(vfname);
        if (result.exists()) {
            return result;
        }
        else {
            return null;
        }
    }
    
    @FXML
    private void storeTestImage(ActionEvent event) {
        int width = (int) hiddenVideo.getBoundsInParent().getWidth();
        int height = (int) hiddenVideo.getBoundsInParent().getHeight();
        WritableImage wim = new WritableImage(width, height);
        
        hiddenVideo.snapshot(null, wim);
        BufferedImage bImage = SwingFXUtils.fromFXImage(wim, null);
        String to="D:\\Downloads\\snapshot.png";
        try {
            ImageIO.write(bImage, "png", new File(to));
        } catch (IOException ex) {}
    }
    
    
    @FXML
    private void selectSource(ActionEvent event) {
        
        if (vc != null) {
            vc.stop();
        }
        
        vc = new VideoController(this);
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Specify input source");
        File infile = fileChooser.showOpenDialog(Facerec.currentStage);
        
        if (infile == null) { return; }
        
        String fname = infile.getAbsolutePath();        
        processButton.setDisable(false);
            
        vc.setSource(fname);
        printStatus("Source set as \""+fname+"\", total frames: "+String.valueOf(vc.getTotalFrames())+"\n");
        
        dispatcher.distributeWork(fname);
        //vc.start();
    }
    
    public void setSourceCLI(String filename) {
        vc = new VideoController(this); 
        vc.setSource(filename);
        dispatcher.distributeWork(filename);
    }
    
    
    public void discoverCLI() {
        discover(null);
    }
    
    public void finish() {
        if (Facerec.GUI_ENABLED) {
            // todo
        }
        else {
            Facerec.cliFinish();
        }
    }
    
    @FXML
    private void selectWebcam(ActionEvent event) {
        
        if (vc != null) {
            vc.stop();
        }
        
        vc = new VideoController(this);
        
        printStatus("Source set as webcam\n");
        
        processButton.setDisable(false);
        
        vc.setSource(0);
        //vc.start();
    }
    
    @FXML
    private void forceCloseVC(ActionEvent event) {
        
        if (vc != null) {
            vc.shutdown();
            processButton.setText("Start Processing");
        }
    }
    
    @FXML
    public void displaySwitch(ActionEvent evt){
    }

    
    @FXML
    public void openVideoWindow(ActionEvent evt) {
        
        // choose data file
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose data file");
        File dataFile = fileChooser.showOpenDialog(Facerec.currentStage);
        
        if (dataFile == null) { return; }
        
        File videoFile = correspondingVideoFile(dataFile.getAbsolutePath());
        if (videoFile == null) {
            fileChooser = new FileChooser();
            fileChooser.setTitle("Choose corresponding video file (optional)");
            videoFile = fileChooser.showOpenDialog(Facerec.currentStage);
        }
        
        
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
            vwStage.setTitle("Video");
            vwStage.setScene(new Scene(root));
            vwStage.initModality(Modality.WINDOW_MODAL);
            vwStage.initOwner( ((Button) evt.getSource()).getScene().getWindow() );
            vwStage.show();

        } catch (IOException e) {e.printStackTrace();}
    }
    
    @FXML
    public void openDBWindow(ActionEvent evt) {

        // open window
        try {
            Parent root = FXMLLoader.load(DBWindowController.class.getResource("dbwindow.fxml"));
            dbwStage = new Stage();
            dbwStage.setTitle("Face Database");
            dbwStage.setScene(new Scene(root));
            dbwStage.initOwner( ((Button) evt.getSource()).getScene().getWindow() );
            dbwStage.show();

        } catch (IOException e) {e.printStackTrace();}
    }
    
    
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Controller.currentController = this;
        
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
