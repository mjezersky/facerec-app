
package facerec;

import java.util.concurrent.Semaphore;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;

/**
 * Class for connected worker handling and management.
 * @author Matous Jezersky
 */
public class WorkerPool {
    
    private ObservableList<Worker> pool;
    private ListView guiElement;
    private static final Semaphore poolAccessSem = new Semaphore(1);
    
    /**
     * Default constructor.
     * @param guiElement assigned list view to view workers in
     */
    public WorkerPool(ListView guiElement) {
        pool = FXCollections.observableArrayList();
        this.guiElement = guiElement;
    }
    
    /**
     * Returns a list of available workers.
     * @return list of available workers
     */
    public ObservableList<Worker> getWorkers() { return pool; }
    
    /**
     * Retrieves worker by name, or null if not found.
     * @param workerName name of the worker
     * @return worker with provided name, or null if not found
     */
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
    
    /**
     * Retrieves worker by index in worker pool.
     * @param index worker index
     * @return worker on given index
     */
    public Worker get(int index) {
        Worker w = pool.get(index);
        return w;
    }
    
    /**
     * Returns true if worker pool is empty.
     * @return true if worker pool is empty, false otherwise
     */
    public boolean isEmpty() { return pool.isEmpty(); }
    
    public void clear() {
        try { poolAccessSem.acquire(); }
        catch (InterruptedException ex) {}
        pool.clear();
        if (Facerec.GUI_ENABLED) { guiElement.setItems(pool); }
        poolAccessSem.release();
    }
    
    /**
     * Adds a worker from a non-GUI thread.
     * @param name worker name to add
     * @param vc assigned video controller
     */
    public void addWorkerNG(String name, VideoController vc) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try { poolAccessSem.acquire(); }
                catch (InterruptedException ex) {}
                try { 
                    if (get(name) != null ) {
                        Facerec.info("Worker ID conflict, got multiple discovery responses for \""+name+"\".");
                    }
                    else {
                        pool.add(new Worker(name, vc));
                    }

                    if (Facerec.GUI_ENABLED) { 
                        guiElement.setItems(pool); }

                    }
                catch (Exception ex) { } // can occur when Discover button is clicked too much, ignore
                poolAccessSem.release();
            }
        });
    }
    
    /**
     * Adds a worker from a GUI thread.
     * @param name name of worker to add
     * @param vc assigned video controller
     */
    public void addWorker(String name, VideoController vc) {
        pool.add(new Worker(name, vc));
        if (Facerec.GUI_ENABLED) { guiElement.setItems(pool); }
    }
    
    /**
     * Returns first available worker.
     * @return first worker in pool
     */
    public Worker getDefault() {
        return pool.get(0);
    }
    
    /**
     * Prints out debug info.
     */
    public void printDebugInfo() {
        System.out.print("Worker info: total "+Integer.toString(pool.size())+" /");
        for (Worker w: pool) {
            System.out.print(" ");
            System.out.print(w.hasFinished());
        }
        System.out.print("\n");
    }
}