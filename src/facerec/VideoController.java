
package facerec;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javax.imageio.ImageIO;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import static org.opencv.videoio.Videoio.CAP_PROP_POS_MSEC;
import static org.opencv.videoio.Videoio.CV_CAP_PROP_FRAME_COUNT;
import static org.opencv.videoio.Videoio.CV_CAP_PROP_POS_FRAMES;


public class VideoController {
    private boolean active;
    private VideoCapture capture;
    private Controller guiController;
    private Link link;
    private boolean stopFlag = false;
    private boolean displayFrames = true;
    
    private int frameDelay = 35;
    
    public VideoController(Controller guiController) {
        this.capture = new VideoCapture();
        this.active = false;
        this.guiController = guiController;
        
        this.link = new Link();
        // debug
        link.connect(guiController.getWorkerPool().getDefault().ip, guiController.getWorkerPool().getDefault().port);
    }
    
    public boolean isActive() { return this.active; }
    
    public void setDisplayFrames(boolean value) { displayFrames = value; }
    
    public Link getLink() { return this.link; }
    
    public void stop() {
        link.close();
        stopFlag = true;
    }
    
    public void shutdown() {
        stop();
        active = false;
        capture.release();
    }
    
    public void seekFrame(double frame) {
        capture.set(CV_CAP_PROP_POS_FRAMES, frame);
    }
    
    public void seek(double ms) {
        capture.set(CAP_PROP_POS_MSEC, ms);
    }
    
    public double getTotalFrames() {
        return capture.get(CV_CAP_PROP_FRAME_COUNT);
    }
    
    public void setSource(String filename) {
        capture.release();
        capture = new VideoCapture(filename);
        System.out.println(capture.isOpened());
    }
    
    public void setSource(int sourceid) {
        capture.release();
        capture = new VideoCapture(sourceid);
        System.out.println(capture.isOpened());
    }
    
    public Mat grabFrame() {
        Mat frame = new Mat();
        if (this.capture.isOpened()) {
            if (!this.capture.read(frame)) {
                System.out.println("Reached end");
                if (isActive()) {
                    Platform.runLater(new Runnable() {
                            @Override
                                public void run() {
                                    guiController.switchProcessing(null);
                                    capture.set(CV_CAP_PROP_POS_FRAMES, 0);
                                }
                        });
                }
            }
            
        }
        
        return frame;
    }
    
    public byte[] imageToByteArray(BufferedImage img) {
        byte[] bytes;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", baos);
            baos.flush();
            bytes = baos.toByteArray();
            baos.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        
        return bytes;
    }
    
    public void setTrainingMode(boolean mode) { link.setTrainingMode(mode); }
    
    public void processFrame() {
        Mat frame = grabFrame();
        
        if (!frame.empty()) {
            BufferedImage bimg = matToBufferedImage(frame);
            
            String resp = link.processRequest(imageToByteArray(bimg));
            
            
            
            Image img = SwingFXUtils.toFXImage(bimg, null);
            
            
            Platform.runLater(new Runnable() {
                            @Override
                                public void run() {
                                    if (displayFrames) {
                                        guiController.displayImage(img, resp);
                                    }
                                    guiController.setResponseText(resp);
                                }
                        });
            
        }
        
    }
    
    public void captureSwitch(){
        active = !active;
    }
    
    
    private static BufferedImage matToBufferedImage(Mat original){
	// init
        
	BufferedImage image = null;
	int width = original.width(), height = original.height(), channels = original.channels();
	byte[] sourcePixels = new byte[width * height * channels];
	original.get(0, 0, sourcePixels);
	
	if (original.channels() > 1) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
	}
        else {
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        }
		
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
		
	return image;
}
    
    public void start() {
        // separate non-FX thread
        Thread t = new Thread() {

            // runnable for that thread
            @Override
            public void run() {
                for (;;) {
                    if (stopFlag) {
                        stopFlag = true;
                        break;
                    }
                    
                    if (!active) {
                        try {Thread.sleep(50);}
                        catch (InterruptedException ex) { }
                    }
                    
                    if (active) {
                        processFrame();
                    }
                    //active = false;
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }
}
