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
        private int startFrame = 0;
        private int currFrame = 0;
        private int endFrame = 0;
        
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
        
        public void processNextFrame() {
            if (currFrame < endFrame) {
                vc.processFrame(this.queueName);
            }
            else {
                System.out.println("Worker finished.");
                closeFile();
            }
        }
        
        public void processResult(Result res) {
            System.out.println("Got result response:");
            System.out.println(res.toString());
            writeToFile(res.toString());
        }
        
        public String getQueueName() { return queueName; }
        
        
        private void writeToFile(String data) {
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