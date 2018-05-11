
package facerec;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javax.imageio.ImageIO;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import static org.opencv.videoio.Videoio.CAP_PROP_POS_MSEC;
import static org.opencv.videoio.Videoio.CV_CAP_PROP_FRAME_COUNT;
import static org.opencv.videoio.Videoio.CV_CAP_PROP_POS_FRAMES;


public class VideoController {
    
    private static Semaphore videoSem = new Semaphore(1); // static semaphore for multiple controllers and calls accessing single file
    
    private boolean active;
    private VideoCapture capture;
    private Controller guiController;
    private Link link;
    private MQLink mqlink;
    
    private Mat lastFrameMat = null;
    private double imgChangeThreshold = 0.00002;

    private static double MAX_IMAGE_COLOUR_VALUE = 255;
    
    public VideoController() {
        this.capture = new VideoCapture();
        this.active = false;
    }
    
    public VideoController(MQLink mqlink) {
        this.capture = new VideoCapture();
        this.active = false;
        this.mqlink = mqlink;
    }
    
    public VideoController(Controller guiController) {
        this.capture = new VideoCapture();
        this.active = false;
        this.guiController = guiController;
        
        this.link = new Link();
        

        //MQLink.makeLink();
        this.mqlink = MQLink.getLink();
        try {
            this.mqlink.connect(FacerecConfig.RABBIT_MQ_SERVER_IP, FacerecConfig.RABBIT_MQ_SERVER_PORT);
            this.mqlink.declareQueue("default");
        } catch (IOException | TimeoutException ex) {
            System.err.println("MQlink error - cannot connect");
        }

    }
    
    public boolean isActive() { return this.active; }
    
    
    public Link getLink() { return this.link; }
    
    public void stop() {
        link.close();
    }
    
    public void shutdown() {
        capture.release();
    }
    
    public void close() {
        capture.release();
    }
    
    public void skipFrames(double count) {
        if (count == 0) { return; }
        
        capture.set(CV_CAP_PROP_POS_FRAMES, capture.get(CV_CAP_PROP_POS_FRAMES)+count);
    }
    
    public void seekFrame(double frame) {
        capture.set(CV_CAP_PROP_POS_FRAMES, frame);
    }
    
    public double currFrame() {
        return capture.get(CV_CAP_PROP_POS_FRAMES);
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
    
    public Mat grabFrame(boolean frameSkip) {
        Mat frame = new Mat();
        
        try { videoSem.acquire(); }
        catch (InterruptedException ex) { return frame; }
        
        if (this.capture.isOpened()) {
            if (this.capture.read(frame)) {
                // read ok, skip frames
                if (frameSkip) {
                    skipFrames(FacerecConfig.FRAME_SKIP);
                }
            }
            else {
                // read failed
                System.out.println("Reached end");
                if (isActive()) {
                    capture.set(CV_CAP_PROP_POS_FRAMES, 0);
                }
            }
        }
        else {
            System.err.println("NOT OPEN");
        }
        
        videoSem.release();
        return frame;
    }
    
    public Mat grabFrame() {
        return grabFrame(true);
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
    
    
    //  returns -1 for fail, 0 for ok, 1 for skipped frame
    public int processFrame(String messageQueueName, boolean skipSimilarFrames, int forceFrameNumber) {
        Mat frame = grabFrame();
        
        if (!frame.empty()) {
            
            if (skipSimilarFrames && forceFrameNumber==0 && !changed(frame)) {
                System.out.print((long) currFrame());
                System.out.println(": similar frame skip!");
                
                // increment counter for skipped frames
                Controller.getCurrentController().getDispatcher().incrementFrameCounter();
                return 1;
            }
            
            BufferedImage bimg = matToBufferedImage(frame);
            
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
            try {
                String header;
                if (forceFrameNumber == 0) { header = Long.toString((long)currFrame()); }
                else { header = Integer.toString(forceFrameNumber); }
                header += ",";
                outputStream.write( header.getBytes() );
                outputStream.write( imageToByteArray(bimg) );
                outputStream.flush();
            }
            catch (IOException ex) {
                ex.printStackTrace();
                return -1;
            }
            
            byte request[] = outputStream.toByteArray( );
            
            try {
                mqlink.publish(messageQueueName, request);
            } catch (IOException ex) {
                System.out.println("Send failed");
                return -1;
            }
        }
        else {
            return -1;
        }
        return 0;
        
    }
    
    public int processFrame(String messageQueueName, boolean skipSimilarFrames) {
        return processFrame(messageQueueName, skipSimilarFrames, 0);
    }
    
    public int processFrame(String messageQueueName) {
        return processFrame(messageQueueName, true, 0);
    }
    
    
    
    public void captureSwitch(){
        active = !active;
    }
    
    
    public Image displayFrame(ImageView imview) {
        
        Mat frame = grabFrame(false);
        
        if (!frame.empty()) {
            System.err.println(changed(frame)); // DEBUG!
            Image img = SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
            
            
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    imview.setImage(img);
                }
            });
            return img;
        }
        return null;
    }
    
    // compare with prev mat
    private boolean changed(Mat m) {
        if (lastFrameMat == null) {
            lastFrameMat = m;
            return true;
        }
        
        boolean res;
        double diff = Core.norm(lastFrameMat, m) / (m.size().area()*VideoController.MAX_IMAGE_COLOUR_VALUE);
        
        /*System.out.println(m.size().area()*VideoController.MAX_IMAGE_COLOUR_VALUE*3);
        System.out.println(720*1280*255*3);
        System.out.println(m.get(100, 100)[0]);
        System.out.println(diff);*/
        
        if ( diff > imgChangeThreshold) { res = true; }
        else { res = false; }
        
        lastFrameMat = m;
        return res;
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
    
   
}
