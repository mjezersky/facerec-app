
package facerec.dbwindow;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;


public class FacerecDB {
    
    private ArrayList<String[]> faces;
    
    
    public FacerecDB() {
        faces = new ArrayList();
    }
    
    public ArrayList<String[]> getFaces() { return faces; }
    
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
            ex.printStackTrace();
        }
    }
    
    public void save(String filename) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            for (String[] face : faces) {
                bw.write(face[0]+","+face[1]+"\n");
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    @Override
    public String toString() {
        String res = "";
        for (String[] face : faces) {
            res += face[0]+","+face[1]+"\n";
        }
        return res;
    }
    
    public void addFace(String name, String features) {
        
        if (features.equals("none")) {
            return;
        }
        
        String[] face = new String[2];
        face[0] = name;
        face[1] = features;
        faces.add(face);
    }
    
    public void removeFace(int index) {
        faces.remove(index);
    }
}
