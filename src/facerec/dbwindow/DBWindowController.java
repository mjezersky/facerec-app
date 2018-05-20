package facerec.dbwindow;

import facerec.Controller;
import facerec.Facerec;
import facerec.FacerecConfig;
import facerec.Worker;
import facerec.result.Result;
import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.util.Callback;

/**
 * Controller for DB window and DB management for GUI and CLI.
 * @author Matous Jezersky
 */
public class DBWindowController implements Initializable {
    private DBBridge dbbridge;
    
    private ObservableList<String> dbItemsList;
    private ObservableList<FaceListElement> dbAddList;
    
    private int currentListIndex = 0;
    private Worker worker;
    private String queueName;
    private FacerecDB facedb;
    
    private List<File> addImageFileList = null;
    
    @FXML private ListView dbListView;
    @FXML private ListView dbAddListView;
    @FXML private Button btnAdd;
    @FXML private Button btnDelete;
    @FXML private Button btnProcess;
    @FXML private TextField namePrompt;
    @FXML private Label associatedImages;
    
    /**
     * Handler for a response to DB frame request.
     * @param res result to process
     */
    public void processResponse(Result res) {
        
        // process result
        facedb.addFace(dbAddList.get(currentListIndex).name, res.fragments.get(0).features);
        dbAddList.get(currentListIndex).state = 2;
        changeListCellState(currentListIndex, 2);
        
        // send next image
        currentListIndex++;
        if (currentListIndex >= dbAddList.size()) {
            // finalize, enable buttons again
            facedb.save(FacerecConfig.FACE_DB_FILENAME);
            safeListClear();
            if (Facerec.GUI_ENABLED) { dbAddListView.setItems(dbAddList); }
            setButtonsDisable(false);
            try {
                Controller.getCurrentController().broadcastDB();
            }
            catch (Exception ex) {
                Facerec.warning("Automatic DB refresh failed, try again manually by re-running Discover.");
            }
            if (!Facerec.GUI_ENABLED) { reportCLIEnd(); }
            return;
        }
        changeListCellState(currentListIndex, 1);
        worker.processDBImage(dbAddList.get(currentListIndex).image);
    }
    
    // clears add to DB list
    private void safeListClear() {
        if (Facerec.GUI_ENABLED) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    dbAddList.clear();
                    dbAddListView.refresh();
                }
            });
        }
    }
    
    // disables/enables buttons before and after processing
    private void setButtonsDisable(boolean value) {
        if (Facerec.GUI_ENABLED) {
            btnAdd.disableProperty().set(value);
            btnDelete.disableProperty().set(value);
            btnProcess.disableProperty().set(value);
        }
    }
    
    // report DB adding end to CLI
    private void reportCLIEnd() {
        Facerec.cliFinish();
    }
    
    /**
     * Adds a new image file to process to processing list.
     * @param name name of the person
     * @param filename image file name
     */
    public void listAdd(String name, String filename) {
        FaceListElement face = new FaceListElement(name, filename);
        dbAddList.add(face);
    }
        
    // change cells factory to allow for colour changing
    private void setLVFactory() {
        dbAddListView.setCellFactory(new Callback<ListView<FaceListElement>, ListCell<FaceListElement>>() {
                    @Override
                    public ListCell<FaceListElement> call(ListView<FaceListElement> myObjectListView) {
                        ListCell<FaceListElement> cell = new ListCell<FaceListElement>(){
                            @Override
                            protected void updateItem(FaceListElement elem, boolean b) {
                                super.updateItem(elem, b);
                                if(elem != null) {
                                    setText(elem.toString());
                                    if(elem.state == 1) {
                                        this.getStyleClass().remove("operation-complete");
                                        this.getStyleClass().add("operation-processing");
                                    } else if(elem.state == 2) {
                                        this.getStyleClass().remove("operation-processing");
                                        this.getStyleClass().add("operation-complete");
                                    } else {
                                        this.getStyleClass().remove("operation-processing");
                                        this.getStyleClass().remove("operation-complete");
                                    }
                                }
                                else {
                                    this.getStyleClass().remove("operation-processing");
                                    this.getStyleClass().remove("operation-complete");
                                    setText("");
                                }                
                            }
                        };

                        return cell;
                    }
                });
    }
    
    // change cell colour
    private void changeListCellState(int index, int state) {
        dbAddList.get(index).state = state;
        
        // list view force update
        if (Facerec.GUI_ENABLED) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    dbAddListView.refresh();
                }
            });
        }
    }
    
    // delete from DB add list
    @FXML private void addListDelete(Event evt) {
        if (dbAddListView.getSelectionModel().isEmpty()) { return; }
        ObservableList<Integer> indices = dbAddListView.getSelectionModel().getSelectedIndices();
        
        // sort indices in descending order, so the list indices don't shift on delete
        List<Integer> sortedIndices = indices.stream().collect(Collectors.toList());
        sortedIndices.sort(Collections.reverseOrder());
        
        for (int i : sortedIndices) {
            dbAddList.remove(i);
        }
        
        dbAddListView.refresh();
    }
    
    // delete from DB view list
    @FXML private void delSelected(Event evt) {
        if (dbListView.getSelectionModel().isEmpty()) { return; }
        ObservableList<Integer> indices = dbListView.getSelectionModel().getSelectedIndices();
        
        // sort indices in descending order, so the list indices don't shift on delete
        List<Integer> sortedIndices = indices.stream().collect(Collectors.toList());
        sortedIndices.sort(Collections.reverseOrder());
        
        for (int i : sortedIndices) {
            facedb.removeFace(i);
        }
        facedb.save(FacerecConfig.FACE_DB_FILENAME);
        refreshDBListView(null);
        
        // refresh DB for already connected workers after deletion
        try {
            Controller.getCurrentController().broadcastDB();
        }
        catch (Exception ex) {
            Facerec.warning("Automatic DB refresh failed, try again manually by re-running Discover.");
        }
    }
    
    // refresh GUI element
    @FXML private void refreshDBListView(Event evt) {
        if (facedb == null) { return; }
        
        dbItemsList.clear();
        for (String[] face : facedb.getFaces()) {
            dbItemsList.add(face[0]+" \t\t"+face[1]);
        }
    }
    
    /**
     * Processes names and assigned images in DB add list.
     */
    @FXML public void processImages() {
        // ! add check for empty pool
        if (Controller.getCurrentController().getWorkerPool().isEmpty()) {
            Facerec.error("No workers available.");
            return;
        }
        worker = Controller.getCurrentController().getWorkerPool().get(0);

        if (dbAddList.isEmpty()) { return; }
        setButtonsDisable(true);
        currentListIndex = 0;
        worker.initVideoController(dbbridge.getMQLink());

        changeListCellState(0, 1);
        worker.processDBImage(dbAddList.get(0).image);
    }
    
    // add name and images to DB add table
    @FXML private void addToTable(ActionEvent evt) {
        String name = namePrompt.getText();
        if (addImageFileList == null) {
            Facerec.error("No images selected");
            return;
        }
        if (name.isEmpty()) {
            Facerec.error("No name set");
            return;
        }
        for (File f : addImageFileList) {
            listAdd(name, f.getAbsolutePath());
        }
        namePrompt.setText("");
        addImageFileList = null;
        associatedImages.setText("Associated images: 0");
    }
    
    // select images for a person
    @FXML private void selectImages(ActionEvent evt) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Specify input source");
        addImageFileList = fileChooser.showOpenMultipleDialog(Controller.getCurrentController().getDBWStage());
        if (addImageFileList != null) {
            associatedImages.setText("Associated images: "+Integer.toString(addImageFileList.size()));
        }
        
    }
    
    /**
     * Initializer for DB Window controller.
     * @param url JavaFX parameter, can be null when not using GUI
     * @param rb JavaFX parameter, can be null when not using GUI
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dbItemsList = FXCollections.observableArrayList();
        dbAddList = FXCollections.observableArrayList();
        
        if (Facerec.GUI_ENABLED) {
            dbListView.setItems(dbItemsList);
            dbAddListView.setItems(dbAddList);
            setLVFactory();
            dbListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        }
        
        dbbridge = Controller.getCurrentController().getDBBridge();
        dbbridge.registerDBWController(this);
        
        facedb = dbbridge.getFaceDB();
        
        refreshDBListView(null);
        
        
    }
    
    
}
