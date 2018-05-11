package facerec.dbwindow;

import facerec.Controller;
import facerec.FacerecConfig;
import facerec.Worker;
import facerec.result.Result;
import java.io.File;
import java.io.IOException;
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
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.Callback;


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
    
    public void processResponse(Result res) {
        
        // process result
        System.out.println("added");
        facedb.addFace(dbAddList.get(currentListIndex).name, res.fragments.get(0).features);
        dbAddList.get(currentListIndex).state = 2;
        changeListCellState(currentListIndex, 2);
        
        // send next image
        currentListIndex++;
        if (currentListIndex >= dbAddList.size()) {
            // finalize, enable buttons again
            facedb.save(FacerecConfig.FACE_DB_FILENAME);
            safeListClear();
            dbAddListView.setItems(dbAddList);
            setButtonsDisable(false);
            try {
                Controller.getCurrentController().broadcastDB();
            }
            catch (IOException ex) {
                System.err.println("Automatic DB refresh failed, try again manually.");
            }
            return;
        }
        changeListCellState(currentListIndex, 1);
        worker.processDBImage(dbAddList.get(currentListIndex).image);
    }
    
    private void safeListClear() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                dbAddList.clear();
                dbAddListView.refresh();
            }
        });
    }
    
    private void setButtonsDisable(boolean value) {
        btnAdd.disableProperty().set(value);
        btnDelete.disableProperty().set(value);
        btnProcess.disableProperty().set(value);
    }
    
    public void listAdd(String name, String filename) {
        FaceListElement face = new FaceListElement(name, filename);
        dbAddList.add(face);
    }
        
    
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
                                        System.out.println(Integer.toString(elem.state)+ " " + elem.toString());
                                        this.getStyleClass().add("operation-processing");
                                    } else if(elem.state == 2) {
                                        System.out.println(Integer.toString(elem.state)+ " " + elem.toString());
                                        this.getStyleClass().remove("operation-processing");
                                        this.getStyleClass().add("operation-complete");
                                    }
                                }
                                else {
                                    setText("");
                                }                
                            }
                        };

                        return cell;
                    }
                });
    }
    
    private void changeListCellState(int index, int state) {
        dbAddList.get(index).state = state;
        
        // list view force update
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                dbAddListView.refresh();
            }
        });
    }
    
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
    }
    
    @FXML private void refreshDBListView(Event evt) {
        if (facedb == null) { return; }
        System.out.println(facedb.getFaces().size());
        dbItemsList.clear();
        for (String[] face : facedb.getFaces()) {
            dbItemsList.add(face[0]+" \t\t"+face[1]);
        }
    }
    
    @FXML private void processImages() {
        // ! add check for empty pool
        if (Controller.getCurrentController().getWorkerPool().isEmpty()) {
            System.err.println("Error: DBWindowController - empty worker pool.");
            return;
        }
        setButtonsDisable(true);
        worker = Controller.getCurrentController().getWorkerPool().get(0);

        if (dbAddList.isEmpty()) { return; }
        currentListIndex = 0;
        worker.initVideoController(dbbridge.getMQLink());

        changeListCellState(0, 1);
        worker.processDBImage(dbAddList.get(0).image);
    }
    
    @FXML private void addToTable(ActionEvent evt) {
        String name = namePrompt.getText();
        if (addImageFileList == null) {
            System.err.println("Error: no images selected");
            return;
        }
        if (name.isEmpty()) {
            System.err.println("Error: no name set");
            return;
        }
        for (File f : addImageFileList) {
            listAdd(name, f.getAbsolutePath());
        }
        namePrompt.setText("");
        addImageFileList = null;
        associatedImages.setText("Associated images: 0");
    }
    
    @FXML private void selectImages(ActionEvent evt) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Specify input source");
        addImageFileList = fileChooser.showOpenMultipleDialog(Controller.getCurrentController().getDBWStage());
        if (addImageFileList != null) {
            associatedImages.setText("Associated images: "+Integer.toString(addImageFileList.size()));
        }
        
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dbItemsList = FXCollections.observableArrayList();
        dbAddList = FXCollections.observableArrayList();
        dbListView.setItems(dbItemsList);
        dbAddListView.setItems(dbAddList);
        setLVFactory();
        dbListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        dbbridge = Controller.getCurrentController().getDBBridge();
        dbbridge.registerDBWController(this);
        
        facedb = dbbridge.getFaceDB();
        
        refreshDBListView(null);
        
        
    }
    
    
}
