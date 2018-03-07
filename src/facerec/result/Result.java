
package facerec.result;

import java.util.ArrayList;

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
        
        //type$workerName$frame$rectangle,person,confidence,feature_vector_or_empty_string$...
        String[] strFragments = msg.split("$");
        
        if (strFragments.length < 4) {
            System.err.println("Result.parseResult - Error: invalid string.");
            return res;
        }
        
        res.workerName = strFragments[1];
        try {
            res.frame = Double.parseDouble(strFragments[2]);
        }
        catch (NumberFormatException ex) {
            System.err.println("Result.parseResult - Error: invalid string.");
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

}
