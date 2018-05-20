
package facerec.result;

import facerec.FacerecConfig;
import facerec.dbwindow.FacerecDB;
import facerec.flow.FlowRecorder;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Class for processing output files.
 * @author Matous Jezersky
 */
public class RawFileReader {
    
    private double lastFrame;
    private Result res;
    private FlowRecorder fr;
    private VectorClusterList vcl;
    private SearchRewriter rewriter;
    
    /**
     * Default constructor.
     * @param fr FlowRecorder to use in processing lines
     */
    public RawFileReader(FlowRecorder fr) {
        this.lastFrame = -1;
        this.res = null;
        this.fr = fr;
        this.vcl = new VectorClusterList();
        this.rewriter = null;
    }
    
    /**
     * Sets search rewriter.
     * @param rewriter search rewriter
     */
    public void setRewriter(SearchRewriter rewriter) {
        this.rewriter = rewriter;
    }
    
    /**
     * Feeds a line into the reader.
     * @param line line from file
     * @throws IOException
     */
    public void feed(String line) throws IOException {
        if (line == null) {
            System.err.println("RawFileReader.feed - Error: cannot parse null.");
            return;
        }
        
        String[] parts = line.split(",");
        
        ResultFragment fmt = new ResultFragment();
        double frameNum, seconds;
        try {
            frameNum = Double.parseDouble(parts[0]);
            seconds = Double.parseDouble(parts[1]);
            fmt.rectString = parts[2];
            fmt.name = parts[3];
            fmt.confidence = Double.parseDouble(parts[4]);
            fmt.features = parts[5];
        }
        catch (NumberFormatException ex) {
            System.err.println("RawFileReader.feed - Error: invalid string.");
            return;
        }
        
        // if rewriter for advanced search is enabled, use it
        if (rewriter != null) {
            rewriter.rewriteFragment(fmt);
        }
        
        // automatic unknown vector clustering
        if (fmt.name.equals("unknown") && FacerecConfig.UNKNOWN_CLUSTERING_ENABLED) {
            //System.out.println(fmt.features.length());
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
            res.seconds = seconds;
            res.fragments.add(fmt);
            // update last frame number
            this.lastFrame = frameNum;
        }
    }
    
    /**
     * Finalizes processing and processes last unprocessed result if there is any. This has to be called each time after RawFileReader processes a file.
     * @throws IOException
     */
    public void flush() throws IOException {
        if (res != null) { fr.feed(res); }
    }
    
    /**
     * Merge temporary worker output files into one.
     * @param outFileName output file name
     * @param inFiles list of temp files
     * @throws IOException
     */
    public static void mergeFiles(String outFileName, String[] inFiles) throws IOException {
        FileWriter fw = new FileWriter(outFileName, false);
        
        for (String fname : inFiles ) {
            BufferedReader br = new BufferedReader(new FileReader(fname));
            String line;
            
            while ((line = br.readLine()) != null) {
                // write current frame
                fw.write(line+"\n");
            }
            fw.flush();
            br.close();
        }
        fw.flush();
        fw.close();
    }
    
    /**
     * Processes an output file and writes it in Flows format.
     * @param inFileName file to process
     * @param outFileName file to write into
     * @throws IOException
     */
    public static void processRawFile(String inFileName, String outFileName) throws IOException {
        
        RawFileReader rfr = new RawFileReader(new FlowRecorder(outFileName));
        
        try (BufferedReader br = new BufferedReader(new FileReader(inFileName))) {
            String line;
            while ((line = br.readLine()) != null) {
               rfr.feed(line);
            }
            rfr.flush();
            rfr.fr.flush();
        }
    }
    
    /**
     * Reads an output file and converts it into Flows.
     * @param inFileName file to process
     * @param filterName search for this name if set, don't if null
     * @param filterDB face DB for if filterName is specified
     * @return Flows from the given file
     * @throws IOException
     */
    public static FlowRecorder openRawAsFlow(String inFileName, String filterName, FacerecDB filterDB) throws IOException {
        
        
        RawFileReader rfr = new RawFileReader(new FlowRecorder(null));
        if (filterName != null) {
            rfr.setRewriter(new SearchRewriter(filterName, filterDB));
        }
        
        try (BufferedReader br = new BufferedReader(new FileReader(inFileName))) {
            String line;
            while ((line = br.readLine()) != null) {
               rfr.feed(line);
            }
            rfr.flush();
            rfr.fr.flushBuffer();
            return rfr.fr;
        }
        
    }
    
    /**
     * Reads an output file and converts it into Flows.
     * @param inFileName file to process
     * @return Flows from the given file
     * @throws IOException
     */
    public static FlowRecorder openRawAsFlow(String inFileName) throws IOException {
        return openRawAsFlow(inFileName, null, null);
    }
    
    /**
     * Reads a file with lines in Flow format and loads them into a FlowRecorder instance.
     * @param inFileName file to process
     * @return Flows from the given file
     * @throws IOException
     */
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
