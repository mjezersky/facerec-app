
package facerec.result;


public class ResultFragment {
    public String name;
    public String rectString;
    public double confidence;
    public String features;
    
    public static ResultFragment parseResultFragment(String strFragment) {
        ResultFragment fmt = new ResultFragment();
        
        // rectangle,person,confidence,feature_vector_or_empty_string
        String[] parts = strFragment.split(",");
        
        try {
                fmt.rectString = parts[0];
                fmt.name = parts[1];
                fmt.confidence = Double.parseDouble(parts[2]);
                fmt.features = parts[3];
            }
            catch (NumberFormatException ex) {
                System.err.println("ResultFragment.parseResultFragment - Error: invalid string.");
            }
        
        return fmt;
    }
    
    @Override
    public String toString() {
        String res = "";

        res += rectString + ",";
        res += name + ",";
        res += Double.toString(confidence) + ",";
        res += features;
        
        return res;
    }
}
