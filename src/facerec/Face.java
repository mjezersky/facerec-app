package facerec;

public class Face {
    public String identifier;
    public double confidence;
    
    public Face(String identifier, double confidence) {
        this.identifier = identifier;
        this.confidence = confidence;
    }
}
