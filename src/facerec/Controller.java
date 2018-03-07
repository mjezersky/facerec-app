package facerec;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.TimeoutException;
import javafx.embed.swing.SwingFXUtils;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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
import javax.imageio.ImageIO;

public class Controller implements Initializable {
    
    @FXML private TextArea statusText;
    @FXML private TextArea responseText;
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
    
    private Rectangle displayRect = null;    
    private VideoController vc = null;
    private MediaPlayer player;
    boolean processing = false;
    boolean displayRectangles = true;
    private MediaView hiddenVideo;
    private WorkerPool workerPool;
    private Dispatcher dispatcher;
    private MQLink mqlink;
    
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
    
    public WorkerPool getWorkerPool() { return workerPool; }
    
    public void printStatus(String str) {
        statusText.setEditable(true);
        statusText.appendText(str);
        statusText.setEditable(false);
    }
    
    
    @FXML
    public void switchProcessing(ActionEvent event) {
        vc.captureSwitch();
        if (vc.isActive()) {
            processButton.setText("Stop Processing");
            printStatus("VC capture started\n");
        }
        else {
            processButton.setText("Start Processing");
            printStatus("VC capture stopped\n");
        }
    }
    
    @FXML
    private void updateLabel(KeyEvent event) {
        vc.getLink().setTrainLabel( ((TextField) event.getSource()).getText() );
    }
    
    @FXML
    private void wipeDB(ActionEvent event) {
        printStatus("DB Wipe request sent\n");
        printStatus("Response " + vc.getLink().sendCommand("WIPEDB") + "\n");
    }
    
    @FXML
    private void dumpDB(ActionEvent event) {
        printStatus("DB Dump request sent\n");
        printStatus("Response " + vc.getLink().dumpDB() + "\n");
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
    
    @FXML
    private void testButton(ActionEvent event) {
        workerPool.clear();
        try {
            mqlink.connect("localhost");
            mqlink.declareExchange("broadcast");
            mqlink.declareQueue("feedback");
            
            mqlink.declareQueue("default");
            mqlink.publish("default", "test");
            
            mqlink.registerDispatcher(dispatcher);
            mqlink.broadcast("broadcast", "DISCOVER");
        }
        catch (IOException | TimeoutException ex) {
            ex.printStackTrace();
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
    
    
    public void setResponseText(String text) {
        responseText.setEditable(true);
        responseText.setText(text);
        responseText.setEditable(false);
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
        vc.setDisplayFrames( ((CheckBox) evt.getSource()).isSelected() );
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        hiddenVideo = new MediaView();
        
        StackPane.setAlignment(processedImage, Pos.TOP_LEFT);
        workerPool = new WorkerPool(workerView);
        //workerPool.addWorker("localhost", 9000);
        
        mqlink = new MQLink();
        
        dispatcher = new Dispatcher(workerPool, mqlink);
        
    }
    
    
}
