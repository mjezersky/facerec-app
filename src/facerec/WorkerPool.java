
package facerec;

import java.util.ArrayList;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;


public class WorkerPool {
    
    private ObservableList<Worker> pool;
    private ListView guiElement;
    
    WorkerPool(ListView guiElement) {
        pool = FXCollections.observableArrayList();
        this.guiElement = guiElement;
    }
    
    public ObservableList<Worker> getWorkers() { return pool; }
    
    
    public Worker get(String workerName) {
        Worker w;
        for (int i=0; i<pool.size(); i++) {
            w = pool.get(i);
            if (w.getQueueName().equals(workerName)) {
                return w;
            }
        }
        return null;
    }
    
    public Worker get(int index) {
        return pool.get(index);
    }
    
    public boolean isEmpty() { return pool.isEmpty(); }
    
    public void clear() {
        pool.clear();
        if (Facerec.GUI_ENABLED) { guiElement.setItems(pool); }
    }
    
    public void addWorkerNG(String name, VideoController vc) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                pool.add(new Worker(name, vc));
                if (Facerec.GUI_ENABLED) { guiElement.setItems(pool); }
            }
        });
    }
    
    public void addWorker(String name, VideoController vc) {
        pool.add(new Worker(name, vc));
        if (Facerec.GUI_ENABLED) { guiElement.setItems(pool); }
    }
    
    public void addWorker(String ip, int port) {
        pool.add(new Worker(ip, port));
        if (Facerec.GUI_ENABLED) { guiElement.setItems(pool); }
    }
    
    public boolean isSelected() {
        return (guiElement.getSelectionModel().selectedIndexProperty().get() != -1);
    }
    
    public String getSelectedIP() {
        int sel = guiElement.getSelectionModel().selectedIndexProperty().get();
        
        if (sel != -1) {
            return pool.get(sel).ip;
        }
        else {
            return null;
        }
    }
    
    public int getSelectedPort() {
        int sel = guiElement.getSelectionModel().selectedIndexProperty().get();
        
        if (sel != -1) {
            return pool.get(sel).port;
        }
        else {
            return -1;
        }
        
    }
    
    public void editSelected(String ip, int port) {
        int sel = guiElement.getSelectionModel().selectedIndexProperty().get();
        
        if (sel != -1) {
            Worker worker = pool.get(sel);
            worker.ip = ip;
            worker.port = port;
            System.out.println(worker);
            pool.set(sel, worker);
        }        
    }
    
    public Worker getDefault() {
        return pool.get(0);
    }
}