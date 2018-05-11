package facerec.flow;

import facerec.RectangleObject;
import facerec.result.Result;
import java.util.Objects;
import org.opencv.core.Mat;


public class Occurrence {
    public String name;
    public int count;

    public double bestFrame;
    public RectangleObject bestFrameRect;
    public double bestConfidence;
    public String bestFrameVecStr;

    public Occurrence(double frameNum, String name, double confidence, RectangleObject rectangle, String vecStr) {
        this.name = name;
        this.count = 1;

        this.bestFrame = frameNum;
        this.bestFrameRect = rectangle;
        this.bestConfidence = confidence;
        this.bestFrameVecStr = vecStr;
    }

    public void feed(double frameNum, double confidence, RectangleObject rectangle, String vecStr) {
        count++;

        if (confidence > this.bestConfidence) {
            this.bestConfidence = confidence;
            this.bestFrameRect = rectangle;
            this.bestFrame = frameNum;
            this.bestFrameVecStr = vecStr;
        }
    }
    
    public Mat getBestFrameVec() {
        return Result.deserializeVec(this.bestFrameVecStr);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) { return false; }
        if (o.getClass() != Occurrence.class) { return false; }
        return this.name.equals(((Occurrence) o).name );
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public String toString() {
        String res = "";
        res += Long.toString((long) bestFrame) + ",";
        res += this.name + ",";
        res += Double.toString(bestConfidence) +",";
        res += this.bestFrameRect.toString();
        return res;
    }
}
