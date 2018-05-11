
package facerec.result;

import java.util.ArrayList;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class Result {
    public String workerName;
    public String filename;
    public double frame;
    public ArrayList<ResultFragment> fragments;
    
    public Result() {
        fragments = new ArrayList();
    }
    
    public static Result parseResult(String msg) {       
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
    
    @Override
    public String toString() {
        String res = "";
        
        for (ResultFragment fmt : this.fragments){
            res += Double.toString(frame) + ",";
            res += fmt.toString() + "\n";
        }
        
        return res;
    }
    
    public static Mat deserializeVec(String vecStr) {
        if (vecStr == null) { return null; }
        if (vecStr.equals("none")) { return null; }
        
        String[] data = vecStr.split("#");
        Mat m = new Mat(1,data.length,CvType.CV_64FC1);
        
        try {
            for (int i=0; i<data.length; i++) {
                m.put(1, i, Double.parseDouble(data[i]));
            }
        }
        catch (NumberFormatException ex) {
            System.err.println("Result.deserializeVec - Error: invalid string.");
            return null;
        }
        
        return m;        
    }
    
    public static double euclideanDist(Mat a, Mat b) {
        return Core.norm(a, b);
    }

}
