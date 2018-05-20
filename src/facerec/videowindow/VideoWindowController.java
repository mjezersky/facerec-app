
package facerec.videowindow;

import facerec.Controller;
import facerec.Facerec;
import facerec.FacerecConfig;
import facerec.RectangleObject;
import facerec.VideoController;
import facerec.flow.Flow;
import facerec.flow.FlowRecorder;
import facerec.flow.ListElementFlow;
import facerec.result.RawFileReader;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;

/**
 * Controller for the video view window.
 * @author Matous Jezersky
 */
public class VideoWindowController implements Initializable {

    @FXML private ImageView imView;
    @FXML private Label progressLabel;
    @FXML private ListView flowList;
    @FXML private Slider videoSlider;
    @FXML private Pane imgContainer;
    @FXML private Canvas frameBar;
    @FXML private Label recognizedLabel;
    @FXML private Label unknownLabel;
    @FXML private Label passesLabel;
    @FXML private Label searchResultLabel;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button clearButton;

    private Rectangle displayRect = null;
    private boolean displayRectangles = true;
    
    private FlowRecorder fr;
    private VideoController vc;
    private ObservableList<ListElementFlow> flows;
    private VideoStatistics vs;
    private double totalFrames;
    private Image currImage;
    private boolean searchSwitch = false;
    
    private static final Color CANVAS_COLOR_UNSELECTED = Color.rgb(68, 68, 229, 0.3);
    private static final Color CANVAS_COLOR_SELECTED = Color.rgb(207, 90, 90, 1);
    
    // display frame in window
    private Image showFrame() {
        if (displayRect != null) {
            imgContainer.getChildren().remove(displayRect);
        }
        
        videoSlider.setValue(vc.currFrame());
        setProgressLabel(vc.currFrame());
        currImage = vc.displayFrame(imView);
                
        return currImage;
    }
    
    // draw rectangle over frame
    private void drawRectangle(Image img, RectangleObject faceRect) {
        if (!displayRectangles) { return; }
        if (faceRect != null) {
            
            
            // assuming the image is landscape-oriented, todo: add actual check for larger dim
            double scaleFactor = imgContainer.getHeight()/img.getHeight();
            RectangleObject scaledRect = new RectangleObject(faceRect);
            scaledRect.scale(scaleFactor);
            
            displayRect = new Rectangle(scaledRect.left, scaledRect.top, scaledRect.right-scaledRect.left, scaledRect.bottom-scaledRect.top);
            displayRect.getStyleClass().add("face-rectangle");
            imgContainer.getChildren().add(displayRect);

        }
    }
    
    // sets frame number display for position in video
    private void setProgressLabel(double frameNum) {
        progressLabel.setText(Long.toString((long) frameNum));
    }
    
    @FXML
    private void play(ActionEvent evt) {
        showFrame();
    }
    
    @FXML
    private void pause(ActionEvent evt) {
        
    }
    
    @FXML
    private void prevFrame(ActionEvent evt) {
        vc.seekFrame(vc.currFrame()-2);
        showFrame();
    }
    
    @FXML
    private void nextFrame(ActionEvent evt) {
        showFrame();
    }
    
    @FXML
    private void seekUpdate(MouseEvent evt) {
        setProgressLabel(videoSlider.getValue());
    }
    
    @FXML
    private void seekConfirm(MouseEvent evt) {
        vc.seekFrame(videoSlider.getValue()-1);
        showFrame();
    }
    
    // stores a snapshot of the currently opened frame
    @FXML
    private void saveSnapshot(ActionEvent evt) {
        if (currImage == null) { return; }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save snapshot as");
        fileChooser.setInitialFileName("snapshot.jpg");
        File outfile = fileChooser.showSaveDialog(Facerec.currentStage);
        if (outfile == null) {
            return;
        }
        BufferedImage bImage = SwingFXUtils.fromFXImage(currImage, null);
        int w = bImage.getWidth();
        int h = bImage.getHeight();
        BufferedImage newImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[] rgb = bImage.getRGB(0, 0, w, h, null, 0, w);
        newImage.setRGB(0, 0, w, h, rgb, 0, w);
        try {
            ImageIO.write(newImage, "jpg", outfile);
        } catch (IOException ex) {
            Facerec.error("Error: Failed to save snapshot.");
        }
    }
    
    @FXML
    private void jumpToStart(ActionEvent evt) {
        ObservableList<ListElementFlow> sel = flowList.getSelectionModel().getSelectedItems();
        if (!sel.isEmpty()) {
            ListElementFlow lef = sel.get(0);
            vc.seekFrame(lef.firstFrame);
            showFrame();
        }
    }
    
    @FXML
    private void jumpToEnd(ActionEvent evt) {
        ObservableList<ListElementFlow> sel = flowList.getSelectionModel().getSelectedItems();
        if (!sel.isEmpty()) {
            ListElementFlow lef = sel.get(0);
            vc.seekFrame(lef.lastFrame);
            showFrame();
        }
    }
    
    @FXML
    private void jumpToBestFrame(ActionEvent evt) {
        ObservableList<ListElementFlow> sel = flowList.getSelectionModel().getSelectedItems();
        if (!sel.isEmpty()) {
            ListElementFlow lef = sel.get(0);
            vc.seekFrame(lef.person.bestFrame);
            Image img = showFrame();
            drawRectangle(img, lef.person.bestFrameRect);
        }
    }
    
    @FXML
    private void selectFlows(MouseEvent evt) {
        ObservableList<ListElementFlow> sel = flowList.getSelectionModel().getSelectedItems();
        
        clearCanvas();
        drawFrames(flows, VideoWindowController.CANVAS_COLOR_UNSELECTED);
        drawFrames(sel, VideoWindowController.CANVAS_COLOR_SELECTED);
    }
    
    // advanced search for a person specified by name in the name field
    @FXML
    private void search(ActionEvent evt) {
        Controller c = Controller.getCurrentController();
        FlowRecorder newFR;
        try {
            if (searchSwitch) {
                newFR = RawFileReader.openRawAsFlow( c.getCurrDataFile() );
            }
            else {
                // while doing specific search, turn off unknown clustering for more accurate results
                boolean tmp = FacerecConfig.UNKNOWN_CLUSTERING_ENABLED;
                FacerecConfig.UNKNOWN_CLUSTERING_ENABLED = false;
                newFR = RawFileReader.openRawAsFlow( c.getCurrDataFile(), searchField.getText(), c.getDBBridge().getFaceDB() );
                // if was enabled previously, re-enable again
                FacerecConfig.UNKNOWN_CLUSTERING_ENABLED = tmp;
            }
        } catch (IOException ex) {
            Facerec.error("Cannot open data file.");
            return;
        }
        fr = newFR;
        
        searchSwitch = !searchSwitch;
        if (searchSwitch) {
            searchButton.setDisable(true);
            clearButton.setDisable(false);
        }
        else {
            searchButton.setDisable(false);
            clearButton.setDisable(true);
        }
        initFlows();
        clearCanvas();
        drawFrames(flows, VideoWindowController.CANVAS_COLOR_UNSELECTED);
                
    }
    
    // initialize Flows for display
    private void initFlows() {
        vs = new VideoStatistics();
        flows = FXCollections.observableArrayList();
        for (Flow f : fr.getFlows()) {
            ListElementFlow lef = new ListElementFlow(f);
            flows.add(lef);
            vs.feed(lef.person);
        }
        flowList.setItems(flows);
        vs.process();
        
        recognizedLabel.setText("Recognized: "+Integer.toString(vs.totalPeople));
        unknownLabel.setText("Unknown: "+Integer.toString(vs.unknownPeople));
        passesLabel.setText("Total passes: "+Integer.toString(vs.totalOccurrences));
    }
    
    // get position in canvas based on a frame number, in order to visualize frames with people occurrences
    private int getCanvasFramePos(double frameNum) {
        return (int) ((frameNum/totalFrames)*frameBar.getWidth());
    }
    
    // visualize occurrences on canvas
    private void drawFrames(ObservableList<ListElementFlow> flowList, Color col) {
        GraphicsContext gc = frameBar.getGraphicsContext2D();
        gc.setFill(col);
        for (ListElementFlow lef : flowList) {
            int startX = getCanvasFramePos(lef.firstFrame);
            int width = getCanvasFramePos(lef.lastFrame) - startX;
            if (width == 0) { width = 1; }
            gc.fillRect(startX, 0, width, frameBar.getHeight());
        }
    }
    
    private void clearCanvas() {
        GraphicsContext gc = frameBar.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, frameBar.getWidth(), frameBar.getHeight());
    }
    
    /**
     * Initializes the controller.
     * @param url JavaFX argument
     * @param rb JavaFX argument
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        fr = Controller.getCurrentController().getVWFlowRecorder();
        vc = Controller.getCurrentController().getVWVideoController();
        totalFrames = vc.getTotalFrames();
        
        videoSlider.setMin(0);
        videoSlider.setMax(totalFrames);
        
        initFlows();
        clearCanvas();
        drawFrames(flows, VideoWindowController.CANVAS_COLOR_UNSELECTED);
        showFrame();
    }
    
}
