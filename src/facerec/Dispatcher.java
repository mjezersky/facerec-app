
package facerec;

import facerec.dbwindow.DBBridge;
import facerec.result.RawFileReader;
import facerec.result.Result;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Semaphore;

/**
 * Class which handles work and message distribution and message handling.
 * @author Matous Jezersky
 */
public class Dispatcher {
    private static final Semaphore discoverySem = new Semaphore(1);
    private static boolean enabled = true; // DEBUG
    
    private WorkerPool workerPool;
    private MQLink mqlink;
    private DBBridge dbbridge;
    
    private final Semaphore counterSemaphore = new Semaphore(1);
    private double totalFrames = 0;
    private double processedFrames = 0;
    private int activeWorkers = 0;
    private double sourceFPS = 30;
    private String outFileName = null;
    private boolean alreadyFinished = false;
    
    /**
     * Default Dispatcher constructor.
     * @param workerPool worker pool to use
     * @param mqlink RabbitMQ link for dispatcher connection
     * @param dbbridge bridge for face DB and DB window
     */
    public Dispatcher(WorkerPool workerPool, MQLink mqlink, DBBridge dbbridge) {
        this.workerPool = workerPool;
        this.mqlink = mqlink;
        this.dbbridge = dbbridge;
    }
    
    /**
     * Returns assigned worker pool.
     * @return assigned worker pool
     */
    public WorkerPool getWorkerPool() { return workerPool; }
    
    /**
     * Shuts down all workers.
     */
    public void stop() {
        for (Worker w : workerPool.getWorkers()) {
            w.closeFile();
            w.reportEnd();
        }
    }
    
    /**
     * Creates output file and halts.
     */
    public void finish() {
        consolidateResults();
        Controller.getCurrentController().finish();
    }
    
    private void updateProgressBar() {
        Controller.getCurrentController().updateProgressBar(processedFrames/ (Double) totalFrames);
    }
    
    /**
     * Returns whether this dispatched has finished processing.
     * @return true if processing was finished, false otherwise
     */
    public boolean hasFinished() {
        return this.alreadyFinished;
    }
    
    private void processDiscoveryResponse(String msg) {
        if (msg.length() < 3) { return; }
        String[] parts = msg.split(",");
        if (parts.length < 1) { System.err.println("Error: got invalid discovery response."); }
        
        try { discoverySem.acquire(); }
        catch (InterruptedException ex) {}
        Facerec.info("Got discovery response from "+parts[1]);
        
        // add worker to pool
        workerPool.addWorkerNG(parts[1], null);
        discoverySem.release();
    }
    
    /**
     * Safely increments the counter of processed frames.
     */
    public void incrementFrameCounter() {
        try { counterSemaphore.acquire(); }
        catch (InterruptedException ex) { return; }
        processedFrames += 1 + FacerecConfig.FRAME_SKIP;
        updateProgressBar();
        counterSemaphore.release();
    }
    
    /**
     * A method for workers to report reaching end of their task.
     */
    public void reportEnd() {        
        // uzilizing counter semaphore for worker concurrency
        try { counterSemaphore.acquire(); }
        catch (InterruptedException ex) {}
        
        if (alreadyFinished) {
            counterSemaphore.release();
            return;
        }
        
        for (Worker w : workerPool.getWorkers()) {
            // a worker hasn't finished, don't finish job yet
            if (!w.hasFinished()) {
                counterSemaphore.release();
                return;
            }
        }
        
        alreadyFinished = true;
        counterSemaphore.release();
        
        // all workers have finished if program reached here
        if (!workerPool.isEmpty()) {
            finish();
        }
        
    }
    
    // handles the message as a video frame processing result
    private void processResult(String msg) {
        // parse and store data into worker's file
        Result res = Result.parseResult(msg, sourceFPS);
        Worker w = workerPool.get(res.workerName);
        
        // handle only if worker is available
        if (w != null) {
            w.processResult(res);
            w.processNextFrame();
        }
        else {
            return;
        }
        incrementFrameCounter();
    }
    
    // handles the message as a frame query for DB
    private void processFrameQuery(String msg) {
        Result res = Result.parseResult(msg, sourceFPS);
        dbbridge.handle(res);
    }
    
    // joins worker output files into one output file
    private void consolidateResults() {
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
    
    /**
     * Distributes work between available workers and begins with video processing.
     * @param filename video file to process
     */
    public void distributeWork(String filename) {
        int workerCount = workerPool.getWorkers().size();
        
        if (FacerecConfig.OUT_FILES_DIR == null) {
            this.outFileName = filename+".csv";
        }
        else {
            File inf = new File(filename);
            File outdir = new File(FacerecConfig.OUT_FILES_DIR);            
            this.outFileName = filename+".csv";
            this.outFileName = outdir.getAbsolutePath()+"/"+inf.getName()+".csv";
        }
        
        if (workerCount < 1) {
            Facerec.error("No workers available!");
            return;
        }
        
        // distribute file among workers
        VideoController vc = new VideoController();
        vc.setSource(filename);
        processedFrames = 0;
        alreadyFinished = false;
        totalFrames = vc.getTotalFrames();
        sourceFPS = vc.getFPS();
        
        
        int step = (int) totalFrames/workerCount;
        
        for (int i=0; i<workerCount; i++) {
            Worker w = workerPool.getWorkers().get(i);
            w.assignWork(filename, i*step, ((i+1)*step)-1, mqlink);
            
            // process by two frames, to have one frame already queued while the other is processing
            w.processNextFrame(false);
            w.processNextFrame(false);
        }
    }
    
    /**
     * Handles received message, callback for RabbitMQ.
     * @param msg received message
     */
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
            Facerec.error("Message handling error.");
        }
    }
    
}
