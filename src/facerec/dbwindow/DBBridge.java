
package facerec.dbwindow;

import facerec.FacerecConfig;
import facerec.MQLink;
import facerec.result.Result;


public class DBBridge {
    
    private DBWindowController controller = null;
    private MQLink mqlink;
    private FacerecDB facedb;
    
    public DBBridge(MQLink mqlink) {
        this.mqlink = mqlink;
        facedb = new FacerecDB();
        facedb.load(FacerecConfig.FACE_DB_FILENAME);
    }
    
    public MQLink getMQLink() { return this.mqlink; }
    
    public void registerDBWController(DBWindowController controller) {
        this.controller = controller;
    }
    
    public void unregisterController() {
        this.controller = null;
    }
    
    public FacerecDB getFaceDB() {
        return this.facedb;
    }
    
    public void handle(Result res) {
        if (this.controller == null) {
            System.err.println("Error: DBBridge - handle called but no controller registered.");
            return;
        }
        System.out.println("calling processResponse");
        this.controller.processResponse(res);
    }
}
