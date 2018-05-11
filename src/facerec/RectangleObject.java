
package facerec;

public class RectangleObject {
    public int left = 0;    // x1
    public int top = 0;     // y1
    public int right = 0;   // x2
    public int bottom = 0;  // y2


    public RectangleObject() {
    }
    
    public RectangleObject(RectangleObject copyFrom) {
        this.left = copyFrom.left;
        this.top = copyFrom.top;
        this.right = copyFrom.right;
        this.bottom = copyFrom.bottom;
    }
    
    public void scale(double factor) {
        left *= factor;
        top *= factor;
        right *= factor;
        bottom *= factor;
    }
    
    public double ratio(RectangleObject r) {
        double intersection = Math.max(0, Math.min(this.right, r.right) - Math.max(this.left, r.left)) * Math.max(0, Math.min(this.bottom, r.bottom) - Math.max(this.top, r.top));
        double union = this.area() + r.area() - intersection;
        
        return intersection/union;
    }
    
    public boolean similar(RectangleObject r, double threshold) {
        if (ratio(r) < threshold) {System.out.print(ratio(r)+" "+this.toString() + " / "+r.toString() + "  ");}
        return ratio(r) >= threshold;
    }
    
    public double area() {
        return (this.right-this.left)*(this.bottom-this.top);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null) { return false; }
        if (o.getClass() != RectangleObject.class) { return false; }
        RectangleObject r = (RectangleObject) o;
        
        return (this.top==r.top && this.right==r.right && this.bottom==r.bottom && this.left==r.left);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 19 * hash + this.left;
        hash = 19 * hash + this.top;
        hash = 19 * hash + this.right;
        hash = 19 * hash + this.bottom;
        return hash;
    }
    
    @Override
    public String toString() {
        String res = "";
        res += Integer.toString(this.left)+"#";
        res += Integer.toString(this.top)+"#";
        res += Integer.toString(this.right)+"#";
        res += Integer.toString(this.bottom);
        return res;
    }

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
