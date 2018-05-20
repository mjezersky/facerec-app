
package facerec.flow;

import facerec.FacerecConfig;
import facerec.RectangleObject;
import facerec.result.Result;
import facerec.result.ResultFragment;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Class for reading frame data and assigning it into Flows (passes).
 * @author Matous Jezersky
 */
public class FlowRecorder {
    
    private ArrayList<Flow> buffer;
    private int maxAge = 10;                 // max flow age
    private double matchThreshold = FacerecConfig.RECTANGLE_MATCH_THRESHOLD;
    private FileWriter fw;
    
    private ArrayList<Flow> flows;
    
    /**
     * Constructor that initializes flow list and buffer, with optional output file name.
     * @param filename optional output file name, set null if not needed
     * @throws IOException 
     */
    public FlowRecorder(String filename) throws IOException {
        this.buffer = new ArrayList();
        this.flows = new ArrayList();
        if (filename != null) {
            this.fw = new FileWriter(filename);
        }
    }
    
    /**
     * Default constructor.
     */
    public FlowRecorder() {
        this.flows = new ArrayList();
    }
    
    /**
     * Add a Flow from Flow string.
     * @param flowString Flow string
     */
    public void addFlow(String flowString) {
        flows.add(Flow.parseFlow(flowString));
    }
    
    private void addFlow(Flow flow) {
        flows.add(flow);
    }
    
    /**
     * Returns list of Flows.
     * @return list of Flows
     */
    public ArrayList<Flow> getFlows() { return flows; }
    
    /**
     * Processes a Result object and assigns the data into a Flow.
     * @param res result object
     * @throws IOException
     */
    public void feed(Result res) throws IOException {
        
        
        for (ResultFragment fmt : res.fragments) {
            RectangleObject r = RectangleObject.deserializeRect(fmt.rectString);
            boolean match = false;
            
            if (fmt.name.equals("none")) { break; }
            
            //System.out.print(Double.toString(res.frame) + ": "+fmt.name+" : ");
            
            for (Flow f : buffer) {
                if (f.match(r, matchThreshold)) {
                    f.feed(res.frame, res.seconds, fmt.name, fmt.confidence, r, fmt.features);
                    match = true;
                    break;
                }
            }
            //System.out.println(match);
            // if no match, create new flow
            if (!match) {
                buffer.add(new Flow(res.frame, res.seconds, fmt.name, fmt.confidence, r, fmt.features));
            }
            
        }
        
        // check age of all flows
        ArrayList<Flow> purgeList = new ArrayList();
        for (Flow f : buffer) {
            double age = Math.abs(res.frame-f.lastFrame);
            if (age>maxAge) {
                // finalize old flows and add to removal queue
                purgeList.add(f);
                //writeFlow(f);
                flows.add(f);
            }
        }
        
        // remove old flows
        for (Flow f : purgeList) {
            buffer.remove(f);
        }
        purgeList.clear();

    }    
    
    /**
     * Flush buffer and merge nearby similar flows if enabled. Doesn't write into a file.
     */
    public void flushBuffer() {
        for (Flow f : buffer) {
            flows.add(f);
        }
        buffer.clear();
        
        if (FacerecConfig.MERGE_NEARBY_FLOWS) {
            FlowMerger fm = new FlowMerger(flows);
            flows = fm.merge();
        }
    }
    
    /**
     * Flush buffer, merge nearby similar flows if enabled, and write data into output file specified in constructor.
     * @throws IOException
     */
    public void flush() throws IOException {
        // finalize all remaining flows
        flushBuffer();
        
        for (Flow f : flows) {
            fw.write(f.toString()+"\n");
        }
        fw.flush();
        
        try { fw.close(); }
        catch (IOException ex) {}
    }
    
    // flushBuffer needs to be called once before this
    /**
     * Write Flows into a file.
     * @param filename file to write into
     * @throws IOException
     */
    public void writeToFile(String filename) throws IOException {
        FileWriter fwriter = new FileWriter(filename);
        for (Flow f : flows) {
            fwriter.write(f.toString()+"\n");
        }
        fwriter.flush();
        
        try { fwriter.close(); }
        catch (IOException ex) {}
        
    }
}
