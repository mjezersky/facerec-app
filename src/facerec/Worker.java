package facerec;

import facerec.result.Result;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Semaphore;

/**
 * Class representing a single worker and its operations.
 * @author Matous Jezersky
 */
public class Worker {   
        private String queueName;
        private VideoController vc;
        private double startFrame = 0;
        private double currFrame = 0;
        private double endFrame = 0;
        private Result lastResult = null;
        private boolean finished = false;
        
        private FileWriter fileWriter = null;
        private final Semaphore fwSem = new Semaphore(1);
        
    /**
     * Default constructor.
     * @param queueName RabbitMQ queue name (worker ID)
     * @param vc assigned video controller
     */
    public Worker(String queueName, VideoController vc) {
        this.queueName = queueName;
        this.vc = vc;
    }

    /**
     * Assigns a section of a video file for worker to process.
     * @param filename file to process
     * @param startFrame process from frame
     * @param endFrame process to frame
     * @param mqlink MQ link to use
     */
    public void assignWork(String filename, int startFrame, int endFrame, MQLink mqlink) {
        this.startFrame = startFrame;
        this.endFrame = endFrame;
        this.currFrame = startFrame;
        this.finished = false;

        vc = new VideoController(mqlink);
        vc.assignWorker(this);
        vc.setSource(filename);
        vc.seekFrame(startFrame);

        try {
            openFile(queueName+".tmp", false);
        }
        catch (IOException ex) {
            System.err.println("Error: Worker failed to open output file.");
        }

    }

    /**
     * Returns the worker's temporary output file name.
     * @return temporary output file name
     */
    public String getFileName() {
        return this.queueName+".tmp";
    }

    /**
     * Initializes assigned controller with MQ link.
     * @param mqlink MQ link to initialize video controller with
     */
    public void initVideoController(MQLink mqlink) {
        vc = new VideoController(mqlink);
    }

    /**
     * Process a single frame for DB.
     * @param filename image to process
     */
    public void processDBImage(String filename) {
        vc.setSource(filename);
        vc.processFrame(this.queueName, false, -1);
        vc.close();
    }

    /**
     * Report reaching end of task.
     */
    public void reportEnd() {
        this.finished = true;
        Controller.getCurrentController().getDispatcher().reportEnd();
    }

    /**
     * Returns true if worker has finished its task, false otherwise.
     * @return true if worker has finished its task, false otherwise
     */
    public boolean hasFinished() {
        return this.finished;
    }

    /**
     * Read and queue next frame for processing.
     * @param skipSimilarFrames if true, will skip similar frames
     */
    public void processNextFrame(boolean skipSimilarFrames) {
        int skippedFrame;
        if (finished) { return; }
        if (currFrame <= endFrame) {
            currFrame = vc.currFrame();
            skippedFrame = vc.processFrame(this.queueName, skipSimilarFrames);
            while (skippedFrame == 1 ) {
                lastResult.frame = vc.currFrame();

                if (lastResult.frame > endFrame) { finished = true; }

                if (finished) {
                    closeAndFinalize();
                    Controller.getCurrentController().getDispatcher().reportEnd();
                    break;
                }

                skippedFrame = vc.processFrame(this.queueName, skipSimilarFrames);
            }

        }
        else {
            //System.out.println("Worker finished.");
            this.finished = true;
            Controller.getCurrentController().getDispatcher().reportEnd();
        }
    }

    /**
     * Read and queue next frame for processing (automatically skips similar frames).
     */
    public void processNextFrame() {
        processNextFrame(true);
    }

    /**
     * Writes a result object into temp output file.
     * @param res result object to write
     */
    public void processResult(Result res) {
        lastResult = res;
        writeToFile(res.toString());
        if (this.finished) {
            closeAndFinalize();
            Controller.getCurrentController().getDispatcher().reportEnd();
        }
    }

    // write last result if not written yet and close temp output file
    private void closeAndFinalize() {
        if (lastResult != null) {
            lastResult.frame=endFrame;
            writeToFile(lastResult.toString());
        }
        closeFile();
    }

    /**
     * Returns MQ queue name (worker ID).
     * @return MQ queue name (worker ID)
     */
    public String getQueueName() { return queueName; }

    // write data to temp output file
    private void writeToFile(String data) {
        if (data == null) { return; }
        if (data.equals("")) { return; }
        if (fileWriter == null) { return; } // DEBUG!
        try { fwSem.acquire(); }
        catch (InterruptedException ex) { return; }

        try {
            fileWriter.write(data);
            fileWriter.flush();

        }
        catch (IOException ex) {}

        fwSem.release();
    }

    
    // open temp output file
    private void openFile(String filename, boolean appendMode) throws IOException {
        if (fileWriter != null) {
            closeFile();
        }
        fileWriter = new FileWriter(filename, appendMode);
    }

    /**
     * Closes temporary output file.
     */
    public void closeFile() {
        if (fileWriter == null) return;
        try {
            fileWriter.close();
        }
        catch (IOException ex) {}
        fileWriter = null;
    }

    /**
     * Closes temporary output file and video file open in assigned video controller.
     */
    public void closeAll() {
        closeFile();
        if (vc != null) {
            vc.close();
        }
    }

    /**
     * Returns MQ queue name (worker ID).
     * @return MQ queue name (worker ID)
     */
    @Override
    public String toString() {
        return queueName;
    }
}