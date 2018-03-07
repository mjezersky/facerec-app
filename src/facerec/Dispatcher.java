
package facerec;

import facerec.result.Result;
import java.util.concurrent.Semaphore;


public class Dispatcher {
    public static final String QUEUE_NAME = "feedback";
    
    private WorkerPool workerPool;
    private MQLink mqlink;
    
    private Semaphore counterSemaphore = new Semaphore(1);
    private double totalFrames = 0;
    private double processedFrames = 0;
    
    public Dispatcher(WorkerPool workerPool, MQLink mqlink) {
        this.workerPool = workerPool;
        this.mqlink = mqlink;
    }
    
    private void processDiscoveryResponse(String msg) {
        if (msg.length() < 3) { return; }
        System.out.println("Got discovery response:");
        System.out.println(msg);
        String[] parts = msg.split(",");
        workerPool.addWorkerNG(parts[1], null);
        // add worker to pool
    }
    
    private void incrementFrameCounter() {
        try { counterSemaphore.acquire(); }
        catch (InterruptedException ex) { return; }
        totalFrames += 1;
        counterSemaphore.release();
    }
    
    private void processResult(String msg) {
        incrementFrameCounter();
        // parse and store data into worker's file
        Result res = Result.parseResult(msg);
        Worker w = workerPool.get(res.workerName);
        if (w != null) {
            w.processResult(res);
            w.processNextFrame();
        }
        else {
            System.out.println("received message but no workers");
        }
    }
    
    private void consolidateResults() {
        System.out.print("Consolidating results of ");
        System.out.print(workerPool.getWorkers().size());
        System.out.println(" worker(s).");
        // join all worker files into one
    }
    
    public void distributeWork(String filename) {
        int workerCount = workerPool.getWorkers().size();
        
        if (workerCount < 1) {
            System.out.println("No workers available!");
            return;
        }
        
        // distribute file into workers
        VideoController vc = new VideoController();
        vc.setSource(filename);
        processedFrames = 0;
        totalFrames = vc.getTotalFrames();
        
        
        int step = (int) totalFrames/workerCount;
        
        for (int i=0; i<workerCount; i++) {
            Worker w = workerPool.getWorkers().get(i);
            w.assignWork(filename, i*step, (i+1)*step, mqlink);
            
            w.processNextFrame();
            w.processNextFrame();
        }
    }
    
    public void processMessage(String msg) {
        if (msg == null) { return; }
        if (msg.length() < 1) { return; }
        
        char msgType = msg.charAt(0);
        
        switch (msgType) {
            case '0':
                processDiscoveryResponse(msg);
                break;
            case '1':
                processResult(msg);
                break;
            default:
                System.err.print("Dispatcher error: unknown message type.");
        }
    }
}
