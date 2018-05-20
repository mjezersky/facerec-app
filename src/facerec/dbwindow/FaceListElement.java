package facerec.dbwindow;

import java.util.Objects;

/**
 * Class to represent name and image pairs in GUI list.
 * @author Matous Jezersky
 */
public class FaceListElement {
    public int state;
    public String name;
    public String image;
   
    /**
     * Default constructor.
     * @param name name of the person
     * @param image image file path
     */
    public FaceListElement(String name, String image) {
        this.name = name;
        this.image = image;
        this.state = 0;
    }
    
    /**
     * Returns a string representation of this object.
     * @return string representation of this object
     */
    @Override
    public String toString() {
        return name + " " + image;
    }

    /**
     * Compares this and another object for equality.
     * @param o object to compare with
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) { return false; }
        if (!o.getClass().equals(FaceListElement.class)) { return false; }
        return ((FaceListElement) o).name.equals(this.name) && ((FaceListElement) o).image.equals(this.image);
    }

    /**
     * Returns this object's hash code.
     * @return hash code
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 43 * hash + Objects.hashCode(this.name);
        hash = 43 * hash + Objects.hashCode(this.image);
        return hash;
    }

    
}
