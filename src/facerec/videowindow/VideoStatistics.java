
package facerec.videowindow;

import facerec.FacerecConfig;
import facerec.flow.Occurrence;
import java.util.ArrayList;


public class VideoStatistics {
    public int totalPeople = 0;
    public int unknownPeople = 0;
    public int totalOccurrences = 0;
    
    private ArrayList<Occurrence> occurrences;
    
    public VideoStatistics() {
        occurrences = new ArrayList();
    }
    
    public void feed(Occurrence oc) {
        for (Occurrence eoc : occurrences) {
            if (eoc.equals(oc)) {
                eoc.count++;
                return;
            }
        }
        // if program reached here, no occurrence was found, create a new one
        Occurrence newoc = new Occurrence(oc.bestFrame, oc.name, oc.bestConfidence, oc.bestFrameRect, oc.bestFrameVecStr);
        occurrences.add(newoc);
    }
    
    public int getOccurrenceCount(String name) {
        for (Occurrence oc : occurrences) {
            if (oc.name.equals(name)) { return oc.count; }
        }
        return 0;
    }
    
    public void process() {
        totalOccurrences = 0;
        totalPeople = 0;
        
        for (Occurrence oc : occurrences) {
            totalOccurrences += oc.count;
            if (oc.name.equals("unknown") || oc.name.contains(FacerecConfig.UNKNOWN_NAME_PREFIX)) { unknownPeople+=oc.count; }
            else { totalPeople++; }
        }
    }
}
