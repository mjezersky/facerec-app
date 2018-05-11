package facerec.result;

import org.opencv.core.Mat;


public class VectorCluster {
    private Mat vecA, vecB, vecC; // three representative vectors
    private double bestConfidence = 0;
    
    public VectorCluster(Mat vec, double confidence) {
        vecA = vecB = vecC = vec;
        bestConfidence = confidence;
    }
    
    public void add(Mat vec, double confidence) {
        vecC = vec;
        if (confidence > bestConfidence) {
            bestConfidence = confidence;
            vecB = vec;
        }
    }
    
    public boolean isMember(Mat vec, double threshold) {
        if (Result.euclideanDist(vecA, vec) <= threshold) { return true; }
        if (Result.euclideanDist(vecB, vec) <= threshold) { return true; }
        if (Result.euclideanDist(vecC, vec) <= threshold) { return true; }
        return false;
    }
}
