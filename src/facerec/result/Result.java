
package facerec.result;

import java.util.ArrayList;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

/**
 * Class representing a single response from a worker, contains data about the frame, and specific detections in the frame in the form of ResultFragments.
 * @author Matous Jezersky
 */
public class Result {
    public String workerName;
    public String filename;
    public double frame;
    public double seconds;
    public ArrayList<ResultFragment> fragments;
    
    /**
     * Default constructor.
     */
    public Result() {
        fragments = new ArrayList();
    }
    
    /**
     * Parses a Result object from a message.
     * @param msg message to parse
     * @param sourceFPS framerate of the source video
     * @return result object
     */
    public static Result parseResult(String msg, double sourceFPS) {       
        Result res = new Result();
        
        //type;workerName;frame;rectangle,person,confidence,feature_vector_or_empty_string;...
        String[] strFragments = msg.split(";");
        
        if (strFragments.length < 4) {
            System.err.println("Result.parseResult - Error: invalid string (bad segment length).");
            System.err.println(strFragments[0]);
            return res;
        }
        
        res.workerName = strFragments[1];
        try {
            res.frame = Double.parseDouble(strFragments[2]);
            res.seconds = res.frame/sourceFPS;
        }
        catch (NumberFormatException ex) {
            System.err.println("Result.parseResult - Error: invalid string.");
            System.err.println(strFragments[2]);
            return res;
        }
        
        for (int i=3; i<strFragments.length; i++) {            
            ResultFragment fmt = ResultFragment.parseResultFragment(strFragments[i]);
            res.fragments.add(fmt);
        }
        
        return res;
    }
    
    /**
     * Returns a string representation of this object.
     * @return string representation of this object
     */
    @Override
    public String toString() {
        String res = "";
        
        for (ResultFragment fmt : this.fragments){
            if (!fmt.name.equals("none")) {
                res += Double.toString(frame) + ",";
                res += Double.toString(seconds) + ",";
                res += fmt.toString() + "\n";
            }
        }
        
        return res;
    }
    
    /**
     * Deserializes a feature vector.
     * @param vecStr serialized feature vector
     * @return feature vector
     */
    public static Mat deserializeVec(String vecStr) {
        if (vecStr == null) { return null; }
        if (vecStr.equals("none")) { return null; }
        
        String[] data = vecStr.split("#");
        Mat m = new Mat(1,data.length,CvType.CV_64FC1);
        
        try {
            for (int i=0; i<data.length; i++) {
                m.put(0, i, Double.parseDouble(data[i]));
            }
        }
        catch (NumberFormatException ex) {
            System.err.println("Result.deserializeVec - Error: invalid string.");
            return null;
        }
        
        return m;        
    }
    
    /**
     * Calculates Euclidean distance between two vectors.
     * @param a vector A
     * @param b vector B
     * @return Euclidean distance between A and B
     */
    public static double euclideanDist(Mat a, Mat b) {
        return Core.norm(a, b);
    }


}
