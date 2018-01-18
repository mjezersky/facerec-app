package facerec;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class Controller implements Initializable {
    
    @FXML private TextArea statusText;
    @FXML private TextArea responseText;
    @FXML private Label imageStatus;
    @FXML private TextField ipAddrIn;
    @FXML private TextField portIn;
    @FXML private ImageView image;
    @FXML private MediaView video;
    @FXML private ImageView processedImage;
    @FXML private SwingNode swingNode;
    @FXML private Button processButton;
    @FXML private ListView workerView;
    
    private VideoController vc = null;
    private MediaPlayer player;
    boolean processing = false;
    private MediaView hiddenVideo;
    private WorkerPool workerPool;
    
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
    
    
    public void displayImage(Image img) {
        processedImage.setImage(img);
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
        printStatus("Source set as \""+fname+"\"\n");
        
        processButton.setDisable(false);
        
        vc.setSource(fname);
        vc.start();
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
        vc.start();
    }
    
    
    @FXML
    public void displaySwitch(ActionEvent evt){
        vc.setDisplayFrames( ((CheckBox) evt.getSource()).isSelected() );
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        hiddenVideo = new MediaView();
        
        workerPool = new WorkerPool(workerView);
        workerPool.addWorker("localhost", 9000);
        
    }    
}
