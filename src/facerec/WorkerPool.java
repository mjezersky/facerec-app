
package facerec;

import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;


public class WorkerPool {
    
    ObservableList<Worker> pool;
    ListView guiElement;
    
    WorkerPool(ListView guiElement) {
        pool = FXCollections.observableArrayList();
        this.guiElement = guiElement;
    }
    
    public void addWorker(String ip, int port) {
        pool.add(new Worker(ip, port));
        guiElement.setItems(pool);
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

    public class Worker {
        public String ip;
        public int port;
        
        Worker(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }
        
        @Override
        public String toString() {
            return ip+":"+String.valueOf(port);
        }
    }
}