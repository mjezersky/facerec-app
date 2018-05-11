
package facerec;

import facerec.dbwindow.DBBridge;
import facerec.result.RawFileReader;
import facerec.result.Result;
import java.io.IOException;
import java.util.concurrent.Semaphore;


public class Dispatcher {
    private static Semaphore primarySem = new Semaphore(0);
    private static boolean enabled = true; // DEBUG
    
    private WorkerPool workerPool;
    private MQLink mqlink;
    private DBBridge dbbridge;
    
    private Semaphore counterSemaphore = new Semaphore(1);
    private double totalFrames = 0;
    private double processedFrames = 0;
    private int activeWorkers = 0;
    private String outFileName = null;
    
    public Dispatcher(WorkerPool workerPool, MQLink mqlink, DBBridge dbbridge) {
        this.workerPool = workerPool;
        this.mqlink = mqlink;
        this.dbbridge = dbbridge;
    }
    
    public WorkerPool getWorkerPool() { return workerPool; }
    
    public static void start() {
        if (enabled) { return; }
        
        enabled = true;
        primarySem.release();
    }
    
    public static void stop() {
        if (!enabled) { return; }
        
        try {
            primarySem.acquire();
            enabled = false;
        } catch (InterruptedException ex) {
            System.err.println("Warning: Dispatcher - semaphore acquire interrupted.");
        }
    }
    
    public static void waitUntilEnabled() {
        if (enabled) { return; } // no need to switch sem if flag is set
        
        // else wait
        try {
            primarySem.acquire();
            primarySem.release();
        } catch (InterruptedException ex) {
            System.err.println("Warning: Dispatcher - semaphore acquire interrupted.");
        }
    }
    
    public static boolean isEnabled() { return enabled; }
    
    public void finish() {
        consolidateResults();
        Controller.getCurrentController().finish();
    }
    
    private void updateProgressBar() {
        Controller.getCurrentController().updateProgressBar(processedFrames/ (Double) totalFrames);
    }
    
    private void processDiscoveryResponse(String msg) {
        if (msg.length() < 3) { return; }
        System.out.println("Got discovery response:");
        System.out.println(msg);
        String[] parts = msg.split(",");
        workerPool.addWorkerNG(parts[1], null);
        // add worker to pool
    }
    
    public void incrementFrameCounter() {
        try { counterSemaphore.acquire(); }
        catch (InterruptedException ex) { return; }
        processedFrames += 1 + FacerecConfig.FRAME_SKIP;
        updateProgressBar();
        counterSemaphore.release();
    }
    
    private void processResult(String msg) {
        // parse and store data into worker's file
        Result res = Result.parseResult(msg);
        Worker w = workerPool.get(res.workerName);
        if (w != null) {
            w.processResult(res);
            w.processNextFrame();
        }
        else {
            System.out.println("received message but no worker "+res.workerName);
            return;
        }
        incrementFrameCounter();
        if (processedFrames >= totalFrames) {
            finish();
        }
    }
    
    private void processFrameQuery(String msg) {
        System.out.println("got DB frame resp");
        Result res = Result.parseResult(msg);
        System.out.println("calling bridge");
        dbbridge.handle(res);
    }
    
    private void consolidateResults() {
        System.out.print("Consolidating results of ");
        System.out.print(workerPool.getWorkers().size());
        System.out.println(" worker(s).");
        // join all worker files into one
        String[] fnames = new String[workerPool.getWorkers().size()];
        for (int i=0; i<workerPool.getWorkers().size(); i++) {
            fnames[i] = workerPool.get(i).getFileName();
        }
        try {
            RawFileReader.mergeFiles(outFileName, fnames);
        }
        catch (IOException ex) {
            System.err.println("Error: cannot write into output file.");
        }
        
        
    }
    
    public void distributeWork(String filename) {
        int workerCount = workerPool.getWorkers().size();
        
        this.outFileName = filename+".csv";
        
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
        System.out.println("workerCount:");
        System.out.println(workerCount);
        System.out.println(totalFrames);
        
        for (int i=0; i<workerCount; i++) {
            Worker w = workerPool.getWorkers().get(i);
            w.assignWork(filename, i*step, ((i+1)*step)-1, mqlink);
            
            w.processNextFrame(false);
            w.processNextFrame(false);
        }
    }
    
    public void processMessage(String msg) {
        try {
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
                case '2':
                    processFrameQuery(msg);
                    break;
                default:
                    System.err.print("Dispatcher error: unknown message type.");
            }
        }
        catch (Exception ex) {
            System.err.println("Message handling error.");
            ex.printStackTrace();
        }
    }
    
}
