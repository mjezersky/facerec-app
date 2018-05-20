
package facerec.dbwindow;

import facerec.Facerec;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

/**
 * Class for face DB reading, writing and management.
 * @author Matous Jezersky
 */
public class FacerecDB {
    
    private ArrayList<String[]> faces;
    
    /**
     * Default constructor.
     */
    public FacerecDB() {
        faces = new ArrayList();
    }
    
    /**
     * Retrieves pairs of faces and serialized feature vectors.
     * @return pairs of faces and serialized feature vectors
     */
    public ArrayList<String[]> getFaces() { return faces; }
    
    /**
     * Loads face DB from given file.
     * @param filename face DB file name
     */
    public void load(String filename) {
        
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitline = line.split(",");
                if (splitline.length != 2) {
                    System.err.println("Error: FacerecDB.load - invalid line in file.");
                    break;
                }
                faces.add(splitline);
            }

        }
        catch (Exception ex) {
            System.err.println("Failed to load face DB.");
        }
    }
    
    /**
     * Saves face DB into given file.
     * @param filename face DB file name
     */
    public void save(String filename) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            for (String[] face : faces) {
                bw.write(face[0]+","+face[1]+"\n");
            }
        }
        catch (Exception ex) {
            Facerec.error("Failed to save face DB.");
        }
    }
    
    /**
     * Returns string representation of the face DB.
     * @returnstring representation of the face DB
     */
    @Override
    public String toString() {
        String res = "";
        for (String[] face : faces) {
            res += face[0]+","+face[1]+"\n";
        }
        return res;
    }
    
    /**
     * Adds a person into the face DB.
     * @param name name of the person
     * @param features serialized feature vector
     */
    public void addFace(String name, String features) {
        
        if (features.equals("none")) {
            return;
        }
        
        String[] face = new String[2];
        face[0] = name;
        face[1] = features;
        faces.add(face);
    }
    
    /**
     * Removes a person by index.
     * @param index index of person to remove
     */
    public void removeFace(int index) {
        faces.remove(index);
    }
}
