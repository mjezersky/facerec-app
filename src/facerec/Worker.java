package facerec;

import facerec.result.Result;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Semaphore;


public class Worker {
        public String ip;
        public int port;
        
        
        private String queueName;
        private VideoController vc;
        private double startFrame = 0;
        private double currFrame = 0;
        private double endFrame = 0;
        private Result lastResult = null;
        private boolean finished = false;
        
        private FileWriter fileWriter = null;
        private Semaphore fwSem = new Semaphore(1);
        
        
        
        Worker(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }
        
        Worker(String queueName, VideoController vc) {
            this.queueName = queueName;
            this.vc = vc;
        }
        
        public void assignWork(String filename, int startFrame, int endFrame, MQLink mqlink) {
            this.startFrame = startFrame;
            this.endFrame = endFrame;
            this.currFrame = startFrame;
            this.finished = false;
            
            System.out.print("Worker assigned frames ");
            System.out.print(startFrame);
            System.out.print(" to ");
            System.out.println(endFrame);
            
            vc = new VideoController(mqlink);
            vc.setSource(filename);
            vc.seekFrame(startFrame);
            
            try {
                openFile(queueName+".tmp", false);
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
            
        }
        
        public String getFileName() {
            return this.queueName+".tmp";
        }
        
        public void initVideoController(MQLink mqlink) {
            vc = new VideoController(mqlink);
        }
        
        public void processDBImage(String filename) {
            vc.setSource(filename);
            vc.processFrame(this.queueName, false, -1);
            vc.close();
        }
        
        public void processNextFrame(boolean skipSimilarFrames) {
            Dispatcher.waitUntilEnabled();
            int skippedFrame;
            System.out.print("PNEXT! "); System.out.print(this.currFrame);
            System.out.print(" "); System.out.println(this.endFrame);
            if (currFrame < endFrame) {
                currFrame = vc.currFrame();
                skippedFrame = vc.processFrame(this.queueName, skipSimilarFrames);
                while (skippedFrame == 1 ) {
                    lastResult.frame = vc.currFrame();
                    processResult(lastResult);
                    skippedFrame = vc.processFrame(this.queueName, skipSimilarFrames);
                }
                
            }
            else {
                System.out.println("Worker finished.");
                this.finished = true;
            }
        }
        
        public void processNextFrame() {
            processNextFrame(true);
        }
        
        public void processResult(Result res) {
            lastResult = res;
            System.out.println("Got result response:");
            System.out.println(res.toString());
            writeToFile(res.toString());
            if (this.finished) {
                closeFile();
            }
        }
        
        public String getQueueName() { return queueName; }
        
        
        private void writeToFile(String data) {
            if (data == null) {
                System.err.println("Error: trying to write null data.");
                return;
            }
            try { fwSem.acquire(); }
            catch (InterruptedException ex) { return; }
            
            try {
                fileWriter.write(data);
                fileWriter.flush();
                
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
            
            fwSem.release();
        }
        
        public void openFile(String filename, boolean appendMode) throws IOException {
            if (fileWriter != null) {
                closeFile();
            }
            fileWriter = new FileWriter(filename, appendMode);
        }
        
        public void closeFile() {
            if (fileWriter == null) return;
            try {
                fileWriter.close();
            }
            catch (IOException ex) {}
            fileWriter = null;
        }
        
        public void closeAll() {
            System.out.println();
            closeFile();
            if (vc != null) {
                vc.shutdown();
            }
        }
        
        @Override
        public String toString() {
            return queueName;
        }
    }