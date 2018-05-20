package facerec.result;

import org.opencv.core.Mat;

/**
 * A class to represent unknown people clusters in a simplistic way.
 * @author Matous Jezersky
 */
public class VectorCluster {
    private Mat vecA, vecB, vecC; // three representative vectors
    private double bestConfidence = 0;
    
    /**
     * Default constructor
     * @param vec first vector
     * @param confidence confidence for the vector
     */
    public VectorCluster(Mat vec, double confidence) {
        vecA = new Mat();
        vecB = new Mat();
        vecC = new Mat();
        vec.copyTo(vecA);
        vec.copyTo(vecB);
        vec.copyTo(vecC);
        bestConfidence = confidence;
    }
    
    /**
     * Add another vector to the cluster.
     * @param vec vector to add
     * @param confidence confidence for the vector
     */
    public void add(Mat vec, double confidence) {
        vec.copyTo(vecC);
        if (confidence > bestConfidence) {
            bestConfidence = confidence;
            vec.copyTo(vecB);
        }
    }
    
    /**
     * Checks whether a vector belongs to this cluster.
     * @param vec vector to check
     * @param threshold tolerance threshold
     * @return true if belongs, false if not
     */
    public boolean isMember(Mat vec, double threshold) {
        if (Result.euclideanDist(vecA, vec) <= threshold) { return true; }
        if (Result.euclideanDist(vecB, vec) <= threshold) { return true; }
        if (Result.euclideanDist(vecC, vec) <= threshold) { return true; }
        return false;
    }
}
