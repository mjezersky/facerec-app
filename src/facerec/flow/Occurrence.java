package facerec.flow;

import facerec.RectangleObject;
import facerec.result.Result;
import java.util.Objects;
import org.opencv.core.Mat;

/**
 * Class representing the occurrences of a single person in a Flow.
 * @author Matous Jezersky
 */
public class Occurrence {
    public String name;
    public int count;

    public double bestFrame;
    public double bestSecond;
    public RectangleObject bestFrameRect;
    public double bestConfidence;
    public String bestFrameVecStr;

    /**
     * Default constructor. Initializes the flow with frame data.
     * @param frameNum frame number
     * @param frameSec second of occurrence
     * @param name recognized name
     * @param confidence recognition confidence
     * @param rectangle bounding box
     * @param vecStr serialized feature vector
     */
    public Occurrence(double frameNum, double frameSec, String name, double confidence, RectangleObject rectangle, String vecStr) {
        this.name = name;
        this.count = 1;

        this.bestFrame = frameNum;
        this.bestSecond = frameSec;
        this.bestFrameRect = rectangle;
        this.bestConfidence = confidence;
        this.bestFrameVecStr = vecStr;
    }

    /**
     * Feeds frame data into the Occurrence object.
     * @param frameNum frame number
     * @param frameSec second of occurrence
     * @param confidence recognition confidence
     * @param rectangle bounding box
     * @param vecStr serialized feature vector
     */
    public void feed(double frameNum, double frameSec, double confidence, RectangleObject rectangle, String vecStr) {
        count++;

        if (confidence > this.bestConfidence) {
            this.bestConfidence = confidence;
            this.bestFrameRect = rectangle;
            this.bestFrame = frameNum;
            this.bestSecond = frameSec;
            this.bestFrameVecStr = vecStr;
        }
    }
    
    /**
     * Returns the serialized feature vector in the best frame.
     * @return serialized feature vector in the best frame
     */
    public Mat getBestFrameVec() {
        return Result.deserializeVec(this.bestFrameVecStr);
    }
    

    /**
     * Compares this object with another for equality.
     * @param o object to compare with
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) { return false; }
        if (o.getClass() != Occurrence.class) { return false; }
        return this.name.equals(((Occurrence) o).name );
    }

    /**
     * Returns a hash code of this object.
     * @return hash code
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.name);
        return hash;
    }

    /**
     * Returns the string representation of this object.
     * @return string representation of this object
     */
    @Override
    public String toString() {
        String res = "";
        res += Long.toString((long) bestFrame) + ",";
        res += Double.toString(bestSecond) + ",";
        res += this.name + ",";
        res += Double.toString(bestConfidence) +",";
        res += this.bestFrameRect.toString();
        return res;
    }
}
