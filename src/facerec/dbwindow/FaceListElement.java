package facerec.dbwindow;

import java.util.Objects;


public class FaceListElement {
    public int state;
    public String name;
    public String image;
   
    public FaceListElement(String name, String image) {
        this.name = name;
        this.image = image;
        this.state = 0;
    }
    
    @Override
    public String toString() {
        return name + " " + image;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) { return false; }
        if (!o.getClass().equals(FaceListElement.class)) { return false; }
        return ((FaceListElement) o).name.equals(this.name) && ((FaceListElement) o).image.equals(this.image);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 43 * hash + Objects.hashCode(this.name);
        hash = 43 * hash + Objects.hashCode(this.image);
        return hash;
    }

    
}
