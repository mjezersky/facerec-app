
package facerec.result;

import facerec.RectangleObject;
import java.util.ArrayList;


public class RawFileReader {
    
    private double lastFrame;
    private ArrayList<RectangleObject> rects;
    
    public void feed(String line) {
        if (line == null) {
            System.err.println("RawFileReader.feed - Error: cannot parse null.");
            return;
        }
        
        String[] parts = line.split(",");
        int frameNum;
        String person;

        try {
            
        }
        catch (NumberFormatException ex) {
            System.err.println("Result.parseResult - Error: invalid string.");
        }
    }
}
