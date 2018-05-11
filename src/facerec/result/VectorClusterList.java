
package facerec.result;

import facerec.FacerecConfig;
import java.util.ArrayList;
import org.opencv.core.Mat;

public class VectorClusterList {
    
    ArrayList<VectorCluster> list;
    
    public VectorClusterList() {
        list = new ArrayList();
    }
    
    // returns name of the cluster (unknown person)
    public String process(Mat vec, double confidence) {
        for (int i=0; i<list.size(); i++) {
            VectorCluster cluster = list.get(i);
            if (cluster.isMember(vec, confidence)) {
                cluster.add(vec, confidence);
                return FacerecConfig.UNKNOWN_NAME_PREFIX+String.valueOf(i);
            }
        }
        
        
        // program reached here, vec doesn't belong in any cluster, create new
        list.add(new VectorCluster(vec, confidence));
        return FacerecConfig.UNKNOWN_NAME_PREFIX+String.valueOf(list.size()-1);
    }
    
    public String process(String vecStr, double confidence) {
        if (vecStr.equals("none")) { return "unknown"; }
        return process(Result.deserializeVec(vecStr), confidence);
    }
}
