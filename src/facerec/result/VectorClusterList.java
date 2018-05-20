
package facerec.result;

import facerec.FacerecConfig;
import java.util.ArrayList;
import org.opencv.core.Mat;

/**
 * Class to hold unknown people clusters.
 * @author Matous Jezersky
 */
public class VectorClusterList {
    
    ArrayList<VectorCluster> list;
    
    /**
     * Default constructor
     */
    public VectorClusterList() {
        list = new ArrayList();
    }
    
    /**
     * Processes a vector along with confidence, either adding it to an existing cluster, or creating a new one.
     * @param vec vector to process
     * @param confidence confidence for the vector
     * @return name of the cluster (assigned unknown name)
     */
    public String process(Mat vec, double confidence) {
        for (int i=0; i<list.size(); i++) {
            VectorCluster cluster = list.get(i);
            if (cluster.isMember(vec, FacerecConfig.UNKNOWN_DIST_THRESHOLD)) {
                cluster.add(vec, confidence);
                return FacerecConfig.UNKNOWN_NAME_PREFIX+String.valueOf(i);
            }
        }
        
        
        // program reached here, vec doesn't belong in any cluster, create new
        list.add(new VectorCluster(vec, confidence));
        return FacerecConfig.UNKNOWN_NAME_PREFIX+String.valueOf(list.size()-1);
    }
    
    /**
     * Processes a serialized vector along with confidence, either adding it to an existing cluster, or creating a new one.
     * @param vecStr serialized vector to process
     * @param confidence confidence for the vector
     * @return name of the cluster (assigned unknown name)
     */
    public String process(String vecStr, double confidence) {
        if (vecStr.equals("none")) { return "unknown"; }
        return process(Result.deserializeVec(vecStr), confidence);
    }
}
