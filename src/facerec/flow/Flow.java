
package facerec.flow;

import facerec.RectangleObject;
import facerec.result.Result;
import java.util.ArrayList;
import java.util.Objects;
import org.opencv.core.Mat;


public class Flow {   

    public int age = 0;
    public double firstFrame;
    public double lastFrame;
    public RectangleObject lastRectangle;
    public ArrayList<Occurrence> occurrences;
    
    public Mat firstFrameVec, lastFrameVec;

    public Flow() {
        this.occurrences = new ArrayList();
    }

    public Flow(double frameNum, String name, double confidence, RectangleObject rectangle, String vecStr) {
        this.occurrences = new ArrayList();
        this.occurrences.add(new Occurrence(frameNum, name, confidence, rectangle, vecStr));
        this.firstFrame = this.lastFrame = frameNum;
        this.firstFrameVec = this.lastFrameVec = Result.deserializeVec(vecStr);
        this.lastRectangle = rectangle;

    }

    public static Flow parseFlow(String flowString) {
        Flow f = null;

        try {
            f = new Flow();
            String[] parts = flowString.split(",");

            f.firstFrame = Double.parseDouble(parts[0]);
            f.lastFrame = Double.parseDouble(parts[1]);

            Occurrence oc = new Occurrence(Double.parseDouble(parts[2]),
                                parts[3],
                                Double.parseDouble(parts[4]),
                                RectangleObject.deserializeRect(parts[5]),
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

        return mostFreq;
    }

    public void feed(double frameNum, String name, double confidence, RectangleObject rectangle, String vecStr) {
        age = 0;
        lastFrame = frameNum;
        lastFrameVec = Result.deserializeVec(vecStr);
        lastRectangle = rectangle;


        // find if person exists in occurrences and increment count, or create a new one
        for (Occurrence oc : occurrences) {
            if (oc.name.equals(name)) {
                oc.feed(frameNum, confidence, rectangle, vecStr);
                return;
            }
        }
        // if program reached here, no occurrence was found, create a new one
        occurrences.add(new Occurrence(frameNum, name, confidence, rectangle, vecStr));

    }

    public boolean match(RectangleObject r, double threshold) {
        return this.lastRectangle.similar(r, threshold);
    }

    @Override
    public String toString() {
        String res = "";

        res += Long.toString((long) firstFrame)+",";
        res += Long.toString((long) lastFrame)+",";
        res += mostFrequentOccurrence().toString();

        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) { return false; }
        if (o.getClass() != Flow.class) { return false; }

        return (this.lastRectangle.equals( ((Flow) o).lastRectangle ));
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.lastRectangle);
        return hash;
    }

}
