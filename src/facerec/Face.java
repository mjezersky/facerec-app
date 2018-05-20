package facerec;

/**
 * Class to store face data.
 * @author Matous Jezersky
 */
public class Face {
    public String identifier;
    public double confidence;
    
    public Face(String identifier, double confidence) {
        this.identifier = identifier;
        this.confidence = confidence;
    }
}
