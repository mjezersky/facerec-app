
package facerec;

/**
 * Class to hold rectangle data.
 * @author Matous Jezersky
 */
public class RectangleObject {
    public int left = 0;    // x1
    public int top = 0;     // y1
    public int right = 0;   // x2
    public int bottom = 0;  // y2

    /**
     * Default constructor, initializes rectangle with zeros.
     */
    public RectangleObject() {
    }
    
    /**
     * Copy rectangle data from another rectangle object.
     * @param copyFrom rectangle object to copy from
     */
    public RectangleObject(RectangleObject copyFrom) {
        this.left = copyFrom.left;
        this.top = copyFrom.top;
        this.right = copyFrom.right;
        this.bottom = copyFrom.bottom;
    }
    
    /**
     * Scales the rectangle by given factor.
     * @param factor scale factor
     */
    public void scale(double factor) {
        left *= factor;
        top *= factor;
        right *= factor;
        bottom *= factor;
    }
    
    /**
     * Calculates the overlap of this and another rectangle.
     * @param r rectangle to calculate overlap with
     * @return overlap ratio
     */
    public double ratio(RectangleObject r) {
        double intersection = Math.max(0, Math.min(this.right, r.right) - Math.max(this.left, r.left)) * Math.max(0, Math.min(this.bottom, r.bottom) - Math.max(this.top, r.top));
        double union = this.area() + r.area() - intersection;
        
        return intersection/union;
    }
    
    /**
     * Checks whether this and another rectangle are similar, based on their overlap and given threshold.
     * @param r rectangle to check similarity with
     * @param threshold theshold for overlap ratio
     * @return
     */
    public boolean similar(RectangleObject r, double threshold) {
        return ratio(r) >= threshold;
    }
    
    /**
     * Calculates area of this rectangle.
     * @return area of the rectangle
     */
    public double area() {
        return (this.right-this.left)*(this.bottom-this.top);
    }
    
    /**
     * Check whether this and another rectangle are equal.
     * @param o another rectangle object
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) { return false; }
        if (o.getClass() != RectangleObject.class) { return false; }
        RectangleObject r = (RectangleObject) o;
        
        return (this.top==r.top && this.right==r.right && this.bottom==r.bottom && this.left==r.left);
    }

    /**
     * Returns hash code for this rectangle object.
     * @return hash code
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 19 * hash + this.left;
        hash = 19 * hash + this.top;
        hash = 19 * hash + this.right;
        hash = 19 * hash + this.bottom;
        return hash;
    }
    
    /**
     * Returns the string representation of this object for serialization.
     * @return string representation
     */
    @Override
    public String toString() {
        String res = "";
        res += Integer.toString(this.left)+"#";
        res += Integer.toString(this.top)+"#";
        res += Integer.toString(this.right)+"#";
        res += Integer.toString(this.bottom);
        return res;
    }

    /**
     * Builds a rectangle object from serialized rectangle.
     * @param msgString serialized rectangle string
     * @return rectangle object
     */
    public static RectangleObject deserializeRect(String msgString) {
        RectangleObject rect = new RectangleObject();

        if (msgString == null) {
            System.err.println("RectangleObject.deserializeRect - Error: deserializing null string.");
            return null;
        }
        
        if (msgString.equals("none")) { return null; }
        
        String[] strData = msgString.split("#");
        if (strData.length != 4) {
            System.err.println("RectangleObject.deserializeRect - Error: bad string for deserialization.");
            return null;
        }
        
        try {
            rect.left = Integer.parseInt(strData[0]);
            rect.top = Integer.parseInt(strData[1]);
            rect.right = Integer.parseInt(strData[2]);
            rect.bottom = Integer.parseInt(strData[3]);
        }
        catch (NumberFormatException ex) {
            System.err.println("RectangleObject.deserializeRect - Error: bad number.");
            return null;
        }
        
        return rect;
    }
}
