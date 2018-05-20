
package facerec.flow;

import facerec.result.SearchRewriter;
import java.util.Objects;

/**
 * Class for the graphical representation of a Flow.
 * @author Matous Jezersky
 */
public class ListElementFlow extends Flow {
    
    public Occurrence person;
    
    public ListElementFlow(Flow f) {
        this.firstSecond = f.firstSecond;
        this.lastSecond = f.lastSecond;
        this.firstFrame = f.firstFrame;
        this.lastFrame = f.lastFrame;
        this.lastRectangle = f.lastRectangle;
        this.person = f.mostFrequentOccurrence();
        
        // in the case of rewriting, will have negative distance here
        if (this.person.bestConfidence<0) {
            // negate it and convert to confidence
            this.person.bestConfidence = SearchRewriter.distToConf(-this.person.bestConfidence);
        }
        
    }
    
    /**
     * Compares this object to another, returns true of they are equal, false otherwise.
     * @param o object to compare with
     * @return returns true of they are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) { return false; }
        if (o.getClass() != ListElementFlow.class) { return false; }
        ListElementFlow lef = (ListElementFlow) o;
        
        boolean res = this.firstFrame == lef.firstFrame &&
                this.lastFrame == this.firstFrame &&
                this.person.equals(lef.person);
        
        return res;
    }

    /**
     * Returns this object's hash code.
     * @return hash code
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.person);
        hash = 17 * hash + (int) (Double.doubleToLongBits(this.firstFrame) ^ (Double.doubleToLongBits(this.firstFrame) >>> 32));
        hash = 17 * hash + (int) (Double.doubleToLongBits(this.lastFrame) ^ (Double.doubleToLongBits(this.lastFrame) >>> 32));
        return hash;
    }
    
    /**
     * Converts seconds into a more readable format.
     * @param seconds seconds to convert
     * @return HH:MM:SS string
     */
    public static String getTime(double seconds) {
        int sec = (int) seconds;
        int min = (int) ((seconds/60) % 60);
        int hr   = (int) ((seconds/(60*60)) % 24);
        return String.format("%02d:%02d:%02d", hr, min, sec);
    }
    
    /**
     * Returns a string representation of this object.
     * @return string representation of this object
     */
    @Override
    public String toString() {
        String res;
        
        res = person.name + " "
                + getTime(firstSecond)
                + "-" + getTime(lastSecond)
                + " (" + Long.toString((long) firstFrame)
                + "-" + Long.toString((long) lastFrame) + ")"
                +" (" +  String.format("%.2f", person.bestConfidence) + ")";

        return res;
    }
}
