
package facerec.videowindow;

import facerec.FacerecConfig;
import facerec.flow.Occurrence;
import java.util.ArrayList;

/**
 * Class to hold information about passes and occurrences in a video file.
 * @author Matous Jezersky
 */
public class VideoStatistics {
    public int totalPeople = 0;
    public int unknownPeople = 0;
    public int totalOccurrences = 0;
    
    private ArrayList<Occurrence> occurrences;
    
    /**
     * Default constructor.
     */
    public VideoStatistics() {
        occurrences = new ArrayList();
    }
    
    /**
     * Feed an occurrence object.
     * @param oc occurrence object
     */
    public void feed(Occurrence oc) {
        for (Occurrence eoc : occurrences) {
            if (eoc.equals(oc)) {
                eoc.count++;
                return;
            }
        }
        // if program reached here, no occurrence was found, create a new one
        Occurrence newoc = new Occurrence(oc.bestFrame, oc.bestSecond, oc.name, oc.bestConfidence, oc.bestFrameRect, oc.bestFrameVecStr);
        occurrences.add(newoc);
    }
    
    /**
     * Counts total occurrences of a person specified by name.
     * @param name name of the person
     * @return number of occurrences
     */
    public int getOccurrenceCount(String name) {
        for (Occurrence oc : occurrences) {
            if (oc.name.equals(name)) { return oc.count; }
        }
        return 0;
    }
    
    /**
     * Process all occurrences added by feed method.
     */
    public void process() {
        totalOccurrences = 0;
        totalPeople = 0;
        
        for (Occurrence oc : occurrences) {
            totalOccurrences += oc.count;
            if (oc.name.equals("unknown") || oc.name.contains(FacerecConfig.UNKNOWN_NAME_PREFIX)) { unknownPeople+=oc.count; }
            else { totalPeople++; }
        }
    }
    
    /**
     * Returns a string representation of this object.
     * @return string representation of this object
     */
    @Override
    public String toString() {
        return "Known people: "+Integer.toString(totalPeople)+" Unknown people: "+Integer.toString(unknownPeople)+" Total occurrences: "+Integer.toString(totalOccurrences);
    }
}
