
package facerec.flow;

import facerec.FacerecConfig;
import static java.lang.Math.abs;
import java.util.ArrayList;

/**
 * Class for merging nearby Flows with the same most frequent occurrence.
 * @author Matous Jezersky
 */
public class FlowMerger {
    private final ArrayList<Flow> flows;
    private final ArrayList<Flow> newFlows;
    
    /**
     * Default constructor
     * @param flows flows list
     */
    public FlowMerger(ArrayList<Flow> flows) {
        this.flows = flows;
        this.newFlows = new ArrayList();
    }
    
    /**
     * Merge nearby Flows with the same most frequent occurrence.
     * @return
     */
    public ArrayList<Flow> merge() {
        int windowSize = 5;
        int windowStartIndex = 0;
        newFlows.clear();
        
        // for every flow
        for (Flow f : flows) {
            
            // set window size
            windowStartIndex = newFlows.size() - windowSize;
            if (windowStartIndex < 0) { 
                windowStartIndex = 0;
            }
            
            boolean matched = false;
            
            // for all new flows in window
            for (int nfi=windowStartIndex; nfi<newFlows.size(); nfi++) {
                Flow nf = newFlows.get(nfi);
                
                // if near enough
                if ( (abs(nf.lastFrame - f.firstFrame)<=FacerecConfig.NEARBY_FLOWS_MAX_DIST) || abs(nf.firstFrame - f.lastFrame)<=FacerecConfig.NEARBY_FLOWS_MAX_DIST ) {
                    // if they have similar names
                    if (nf.mostFrequentOccurrence().name.equals(f.mostFrequentOccurrence().name)) {
                        matched = true;
                        // merge
                        if (nf.firstFrame > f.firstFrame) { nf.firstFrame = f.firstFrame; }
                        if (nf.lastFrame < f.lastFrame) { nf.lastFrame = f.lastFrame; }
                        Occurrence nfoc = nf.mostFrequentOccurrence();
                        Occurrence foc = f.mostFrequentOccurrence();
                        
                        if (nfoc.bestConfidence<foc.bestConfidence) {
                            // if the confidence is better, throw away other occurrences and replace with the better one
                            nf.setSingleOccurrence(foc);
                        }
                        
                    }
                }
            }
            
            // if no match in newFlows was found, add flow to newFlows
            if (!matched) {
                newFlows.add(f);
            }
        }
        
        return newFlows;
    }
}
