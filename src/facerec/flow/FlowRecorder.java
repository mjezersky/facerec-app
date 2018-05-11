
package facerec.flow;

import facerec.FacerecConfig;
import facerec.RectangleObject;
import facerec.result.Result;
import facerec.result.ResultFragment;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;


public class FlowRecorder {
    
    private ArrayList<Flow> buffer;
    private int maxAge = 5;                 // max flow age
    private double matchThreshold = FacerecConfig.RECTANGLE_MATCH_THRESHOLD;
    private FileWriter fw;
    
    private ArrayList<Flow> flows;
    
    
    public FlowRecorder(String filename) throws IOException {
        this.buffer = new ArrayList();
        this.flows = new ArrayList();
        this.fw = new FileWriter(filename);
    }
    
    public FlowRecorder() {
        this.flows = new ArrayList();
    }
    
    public void addFlow(String flowString) {
        flows.add(Flow.parseFlow(flowString));
    }
    
    private void addFlow(Flow flow) {
        flows.add(flow);
    }
    
    public ArrayList<Flow> getFlows() { return flows; }
    
    public void feed(Result res) throws IOException {
        
        
        for (ResultFragment fmt : res.fragments) {
            RectangleObject r = RectangleObject.deserializeRect(fmt.rectString);
            boolean match = false;
            
            if (fmt.name.equals("none")) { break; }
            
            System.out.print(Double.toString(res.frame) + ": "+fmt.name+" : ");
            
            for (Flow f : buffer) {
                if (f.match(r, matchThreshold)) {
                    f.feed(res.frame, fmt.name, fmt.confidence, r, fmt.features);
                    match = true;
                    break;
                }
            }
            System.out.println(match);
            // if no match, create new flow
            if (!match) {
                buffer.add(new Flow(res.frame, fmt.name, fmt.confidence, r, fmt.features));
            }
            
        }
        
        // age all flows
        ArrayList<Flow> purgeList = new ArrayList();
        for (Flow f : buffer) {
            f.age++;
            if (f.age>maxAge) {
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
    
    public void flushBuffer() {
        for (Flow f : buffer) {
            flows.add(f);
        }
        buffer.clear();
    }
    
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
}
