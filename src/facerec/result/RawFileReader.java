
package facerec.result;

import facerec.FacerecConfig;
import facerec.flow.FlowRecorder;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class RawFileReader {
    
    private double lastFrame;
    private Result res;
    private FlowRecorder fr;
    private VectorClusterList vcl;
    
    public RawFileReader(FlowRecorder fr) {
        this.lastFrame = -1;
        this.res = null;
        this.fr = fr;
        this.vcl = new VectorClusterList();
    }
    
    public void feed(String line) throws IOException {
        if (line == null) {
            System.err.println("RawFileReader.feed - Error: cannot parse null.");
            return;
        }
        
        String[] parts = line.split(",");
        
        ResultFragment fmt = new ResultFragment();
        double frameNum;
        try {
            frameNum = Double.parseDouble(parts[0]);
            fmt.rectString = parts[1];
            fmt.name = parts[2];
            fmt.confidence = Double.parseDouble(parts[3]);
            fmt.features = parts[4];
        }
        catch (NumberFormatException ex) {
            System.err.println("RawFileReader.feed - Error: invalid string.");
            return;
        }
        
        // automatic unknown vector clustering
        if (fmt.name.equals("unknown") && FacerecConfig.UNKNOWN_CLUSTERING_ENABLED) {
            fmt.name = vcl.process(fmt.features, fmt.confidence);
        }
        
        if (frameNum == lastFrame) {
            // only add to current fragment
            res.fragments.add(fmt);
        }
        else {
            // new frame, finalize old frame and create new
            if (res != null) { fr.feed(res); }
            res = new Result();
            res.frame = frameNum;
            res.fragments.add(fmt);
            // update last frame number
            this.lastFrame = frameNum;
        }
        
        
    }
    
    public static void mergeFiles(String outFileName, String[] inFiles) throws IOException {
        FileWriter fw = new FileWriter(outFileName, false);
        
        
        for (String fname : inFiles ) {
            BufferedReader br = new BufferedReader(new FileReader(fname));
            String line;
            while ((line = br.readLine()) != null) {
                fw.write(line+"\n");
            }
            fw.flush();
            br.close();
        }
        fw.flush();
        fw.close();
    }
    
    
    public static void processRawFile(String inFileName, String outFileName) throws IOException {
        
        RawFileReader rfr = new RawFileReader(new FlowRecorder(outFileName));
        
        try (BufferedReader br = new BufferedReader(new FileReader(inFileName))) {
            String line;
            while ((line = br.readLine()) != null) {
               rfr.feed(line);
            }
            rfr.fr.flush();
        }
    }
    
    public static FlowRecorder openRawAsFlow(String inFileName) throws IOException {
        
        RawFileReader rfr = new RawFileReader(new FlowRecorder("passes.csv"));
        
        try (BufferedReader br = new BufferedReader(new FileReader(inFileName))) {
            String line;
            while ((line = br.readLine()) != null) {
               rfr.feed(line);
            }
            rfr.fr.flushBuffer();
            return rfr.fr;
        }
        
    }
    
    public static FlowRecorder processFlowFile(String inFileName) throws IOException {
        
        FlowRecorder fr = new FlowRecorder();
        
        try (BufferedReader br = new BufferedReader(new FileReader(inFileName))) {
            String line;
            while ((line = br.readLine()) != null) {
               fr.addFlow(line);
            }

        }
        
        return fr;
    }
}
