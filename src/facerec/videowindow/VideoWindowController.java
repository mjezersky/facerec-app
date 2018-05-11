
package facerec.videowindow;

import facerec.Controller;
import facerec.RectangleObject;
import facerec.VideoController;
import facerec.flow.Flow;
import facerec.flow.FlowRecorder;
import facerec.flow.ListElementFlow;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
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

    private Rectangle displayRect = null;
    private boolean displayRectangles = true;
    
    private FlowRecorder fr;
    private VideoController vc;
    private ObservableList<ListElementFlow> flows;
    private VideoStatistics vs;
    private double totalFrames;
    
    private static Color CANVAS_COLOR_UNSELECTED = Color.rgb(68, 68, 229, 0.3);
    private static Color CANVAS_COLOR_SELECTED = Color.rgb(207, 90, 90, 1);
    
    private Image showFrame() {
        if (displayRect != null) {
            imgContainer.getChildren().remove(displayRect);
        }
        
        videoSlider.setValue(vc.currFrame());
        setProgressLabel(vc.currFrame());
        Image img = vc.displayFrame(imView);
                
        return img;
    }
    
    private void drawRectangle(Image img, RectangleObject faceRect) {
        if (!displayRectangles) { return; }
        System.out.println(faceRect.toString());
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
    
    private void setProgressLabel(double frameNum) {
        progressLabel.setText(Long.toString((long) frameNum));
    }
    
    @FXML
    public void play(ActionEvent evt) {
        showFrame();
    }
    
    @FXML
    public void pause(ActionEvent evt) {
        
    }
    
    @FXML
    public void prevFrame(ActionEvent evt) {
        vc.seekFrame(vc.currFrame()-2);
        showFrame();
    }
    
    @FXML
    public void nextFrame(ActionEvent evt) {
        showFrame();
    }
    
    @FXML
    public void seekUpdate(MouseEvent evt) {
        setProgressLabel(videoSlider.getValue());
    }
    
    @FXML
    public void seekConfirm(MouseEvent evt) {
        vc.seekFrame(videoSlider.getValue()-1);
        showFrame();
    }
    
    @FXML
    public void jumpToStart(ActionEvent evt) {
        ObservableList<ListElementFlow> sel = flowList.getSelectionModel().getSelectedItems();
        if (!sel.isEmpty()) {
            ListElementFlow lef = sel.get(0);
            vc.seekFrame(lef.firstFrame);
            showFrame();
        }
    }
    
    @FXML
    public void jumpToEnd(ActionEvent evt) {
        ObservableList<ListElementFlow> sel = flowList.getSelectionModel().getSelectedItems();
        if (!sel.isEmpty()) {
            ListElementFlow lef = sel.get(0);
            vc.seekFrame(lef.lastFrame);
            showFrame();
        }
    }
    
    @FXML
    public void jumpToBestFrame(ActionEvent evt) {
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
    
    @FXML
    private void search(ActionEvent evt) {
        int count = vs.getOccurrenceCount(searchField.getText());
        
        if (count == 0) {
            searchResultLabel.setText("No occurrence found.");
        }
        else {
            searchResultLabel.setText("Found "+Integer.toString(count)+" occurrences.");
        }
    }
    
    public void initFlows() {
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
    
    
    private int getCanvasFramePos(double frameNum) {
        return (int) ((frameNum/totalFrames)*frameBar.getWidth());
    }
    
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
