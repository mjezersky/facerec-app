
package facerec.flow;

import facerec.RectangleObject;
import facerec.result.Result;
import java.util.ArrayList;
import java.util.Objects;
import org.opencv.core.Mat;

/**
 * Class to represent one pass of a person.
 * @author Matous Jezersky
 */
public class Flow {   

    public double firstFrame;
    public double lastFrame;
    public double firstSecond;
    public double lastSecond;
    public RectangleObject lastRectangle;
    public ArrayList<Occurrence> occurrences;
    
    public Mat firstFrameVec, lastFrameVec;

    /**
     * Default constructor, creates an empty Flow.
     */
    public Flow() {
        this.occurrences = new ArrayList();
    }

    /**
     * Constructor to initialize flow with frame data.
     * @param frameNum frame number
     * @param frameSec second of occurrence
     * @param name recognized name
     * @param confidence recognition confidence
     * @param rectangle bounding box
     * @param vecStr serialized feature vector
     */
    public Flow(double frameNum, double frameSec, String name, double confidence, RectangleObject rectangle, String vecStr) {
        this.occurrences = new ArrayList();
        this.occurrences.add(new Occurrence(frameNum, frameSec, name, confidence, rectangle, vecStr));
        this.firstFrame = this.lastFrame = frameNum;
        this.firstSecond = this.lastSecond = frameSec;
        this.firstFrameVec = this.lastFrameVec = Result.deserializeVec(vecStr);
        this.lastRectangle = rectangle;

    }

    /**
     * Parse Flow from a line.
     * @param flowString line to parse
     * @return parsed Flow
     */
    public static Flow parseFlow(String flowString) {
        Flow f = null;

        try {
            f = new Flow();
            String[] parts = flowString.split(",");

            f.firstFrame = Double.parseDouble(parts[0]);
            f.lastFrame = Double.parseDouble(parts[1]);
            f.firstSecond = Double.parseDouble(parts[2]);
            f.lastSecond = Double.parseDouble(parts[3]);

            Occurrence oc = new Occurrence(Double.parseDouble(parts[4]),
                                Double.parseDouble(parts[5]),
                                parts[6],
                                Double.parseDouble(parts[7]),
                                RectangleObject.deserializeRect(parts[8]),
                                null);

            f.occurrences.add(oc);
            
            f.firstFrameVec = f.lastFrameVec = null;

        }
        catch (NumberFormatException ex) {
            System.err.println("Flow.parseFlow - Error: invalid string.");
            return null;
        }

        return f;            
    }

    /**
     * Returns the most frequently occurring person by name in this Flow.
     * @return most frequent occurrence
     */
    public Occurrence mostFrequentOccurrence() {
        Occurrence mostFreq = null;
        int max = 0;
        double bestFrame = 0;
        double bestConfidence = 0;

        for (Occurrence oc : occurrences) {
            if (oc.count>max) {
                max = oc.count;
                mostFreq = oc;
            }
        }
        
        /*if (mostFreq.bestConfidence > 0) {
            System.out.println(mostFreq.bestFrame);
        }*/
        //System.out.println("Most freq: "+mostFreq.name+" with "+Integer.toString(mostFreq.count));

        return mostFreq;
    }
    
    /**
     * Clear all occurrences and set just one. Used for SearchRewriter.
     * @param oc single occurrence to set
     */
    public void setSingleOccurrence(Occurrence oc) {
        occurrences.clear();
        occurrences.add(oc);
    }

    /**
     * Feed frame data.
     * @param frameNum frame number
     * @param frameSec second of occurrence
     * @param name recognized name
     * @param confidence recognition confidence
     * @param rectangle bounding box
     * @param vecStr serialized feature vector
     */
    public void feed(double frameNum, double frameSec, String name, double confidence, RectangleObject rectangle, String vecStr) {
        lastFrame = frameNum;
        lastSecond = frameSec;
        lastFrameVec = Result.deserializeVec(vecStr);
        lastRectangle = rectangle;

        // find if person exists in occurrences and increment count, or create a new one
        for (Occurrence oc : occurrences) {
            if (oc.name.equals(name)) {
                oc.feed(frameNum, frameSec, confidence, rectangle, vecStr);
                return;
            }
        }
        // if program reached here, no occurrence was found, create a new one
        occurrences.add(new Occurrence(frameNum, frameSec, name, confidence, rectangle, vecStr));

    }

    /**
     * Check whether bounding box belongs to this Flow, using tolerance given by threshold.
     * @param r bounding box to check
     * @param threshold tolerance threshold
     * @return true if bounding box belongs to Flow, false otherwise
     */
    public boolean match(RectangleObject r, double threshold) {
        return this.lastRectangle.similar(r, threshold);
    }

    /**
     * Returns string representation of this Flow.
     * @return string representation of this Flow
     */
    @Override
    public String toString() {
        String res = "";

        res += Long.toString((long) firstFrame)+",";
        res += Long.toString((long) lastFrame)+",";
        res += Long.toString((long) firstSecond)+",";
        res += Long.toString((long) lastSecond)+",";
        res += mostFrequentOccurrence().toString();

        return res;
    }

    /**
     * Compares this Flow with another for exact match.
     * @param o Flow to compare with
     * @return true if they are exactly similar, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) { return false; }
        if (o.getClass() != Flow.class) { return false; }

        return (this.lastRectangle.equals( ((Flow) o).lastRectangle ));
    }

    /**
     * Returns a hash code representation of this object.
     * @return hash code
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.lastRectangle);
        return hash;
    }

}
