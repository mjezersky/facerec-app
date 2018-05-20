
package facerec.dbwindow;

import facerec.FacerecConfig;
import facerec.MQLink;
import facerec.result.Result;

/**
 * Class to bridge Controller, DBWindowController and assigned FacerecDB
 * @author Matous Jezersky
 */
public class DBBridge {
    
    private DBWindowController controller = null;
    private MQLink mqlink;
    private FacerecDB facedb;
    
    /**
     * Default constructor.
     * @param mqlink assigned MQ link
     */
    public DBBridge(MQLink mqlink) {
        this.mqlink = mqlink;
        facedb = new FacerecDB();
        facedb.load(FacerecConfig.FACE_DB_FILENAME);
    }
    
    /**
     * Returns assigned MQ link.
     * @return assigned MQ link
     */
    public MQLink getMQLink() { return this.mqlink; }
    
    /**
     * Registers a DBWindowController.
     * @param controller DBWindowController to register
     */
    public void registerDBWController(DBWindowController controller) {
        this.controller = controller;
    }
    
    /**
     * Unregisters current DBWindowController.
     */
    public void unregisterController() {
        this.controller = null;
    }
    
    /**
     * Retrieves current face database.
     * @return face database
     */
    public FacerecDB getFaceDB() {
        return this.facedb;
    }
    
    /**
     * Result handler for dispatcher, handles responses to DB frame requests.
     * @param res result to process
     */
    public void handle(Result res) {
        if (this.controller == null) {
            System.err.println("Error: DBBridge - handle called but no controller registered.");
            return;
        }
        this.controller.processResponse(res);
    }
}
