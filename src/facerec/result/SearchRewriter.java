
package facerec.result;

import facerec.FacerecConfig;
import facerec.dbwindow.FacerecDB;
import java.util.ArrayList;
import org.opencv.core.Mat;

/**
 * Class for advanced search in output files.
 * @author Matous Jezersky
 */
public class SearchRewriter {
    private FacerecDB facedb;
    private String searchName;
    private ArrayList<Mat> filteredFaces;
    
    /**
     * Default constructor.
     * @param searchName name to search for
     * @param facedb face DB
     */
    public SearchRewriter(String searchName, FacerecDB facedb) {
        this.searchName = searchName;
        this.facedb = facedb;
        this.filteredFaces = new ArrayList();
        
        filterFaces();
    }
    
    // filters faces by name
    private void filterFaces() {
        // create a smaller database of faces, only with those with name equal to searchName
        for (String[] face : facedb.getFaces()) {
            if (face[0].equals(searchName)) {
                filteredFaces.add(Result.deserializeVec(face[1]));
            }
        }
    }
    
    /**
     * Converts distance to confidence.
     * @param dist distance
     * @return confidence
     */
    public static double distToConf(double dist) {
        double conf = 1-((dist-0.4)/0.4);
        if (conf<0) { return 0.0; }
        if (conf>1) { return 1.0; }
        
        return dist;
    }
    
    /**
     * Rewrites a ResultFragment based on the set name filter.
     * @param fmt ResultFragment to rewrite
     */
    public void rewriteFragment(ResultFragment fmt) {
        
        if (fmt.name.equals("none")) { return; }
        
        // preset as unknown
        fmt.name = "unknown";
        
        // deserialize result features
        Mat resVec = Result.deserializeVec(fmt.features);
        
        // compare with filtered db
        double minDist = 100000; // default big number
        for (Mat dbVec : filteredFaces) {
            double dist = Result.euclideanDist(resVec, dbVec);
            if ( dist < minDist ) {  minDist = dist; }
        }
        if (minDist <= FacerecConfig.SEARCH_DIST_THRESHOLD_SOFT) {
                // faces were matched, rewrite
                fmt.name = searchName;
                
                // write negative distance into confidence field (to allow max to work on distances), must be converted before displaying
                fmt.confidence = -minDist;
                System.out.println(searchName);
                System.out.println(minDist);
        }
    }
    
}
