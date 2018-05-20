
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
import static org.opencv.videoio.Videoio.CV_CAP_PROP_FPS;
import static org.opencv.videoio.Videoio.CV_CAP_PROP_FRAME_COUNT;
import static org.opencv.videoio.Videoio.CV_CAP_PROP_POS_FRAMES;

/**
 * Class to handle video and image files and allow seeking and reading frames.
 * @author Matous Jezersky
 */
public class VideoController {
    
    private static Semaphore videoSem = new Semaphore(1); // static semaphore for multiple controllers and calls accessing single file

    private VideoCapture capture;
    private Controller guiController;
    private MQLink mqlink;
    private Worker assignedWorker;
    
    private Mat lastFrameMat = null;
    private double imgChangeThreshold = 0.00002;

    private static double MAX_IMAGE_COLOUR_VALUE = 255;
    
    /**
     * Constructor for local only processing.
     */
    public VideoController() {
        this.capture = new VideoCapture();
    }
    
    /**
     * Constructor for processing with RabbitMQ.
     * @param mqlink RabbitMQ connection
     */
    public VideoController(MQLink mqlink) {
        this.capture = new VideoCapture();
        this.mqlink = mqlink;
    }
    
    /**
     * Constructor for independent, connected controller.
     * @param guiController assigned gui controller
     */
    public VideoController(Controller guiController) {
        this.capture = new VideoCapture();
        this.guiController = guiController;
        
        //this.link = new Link();
        

        //MQLink.makeLink();
        this.mqlink = MQLink.getLink();
        try {
            this.mqlink.connect();
            this.mqlink.declareQueue("default");
        } catch (IOException | TimeoutException ex) {
            System.err.println("MQlink error - cannot connect");
        }

    }
    
    /**
     * Assign a worker to report to when video stream reaches end.
     * @param w worker to report end to
     */
    public void assignWorker(Worker w) {
        assignedWorker = w;
    }
    
    /**
     * Closes the controller and the video file.
     */
    public void close() {
        capture.release();
    }
    
    /**
     * Change current frames to current + count.
     * @param count frames to skip
     */
    public void skipFrames(double count) {
        if (count == 0) { return; }
        
        capture.set(CV_CAP_PROP_POS_FRAMES, capture.get(CV_CAP_PROP_POS_FRAMES)+count);
    }
    
    /**
     * Moves to a given frame in video.
     * @param frame frame number to move to
     */
    public void seekFrame(double frame) {
        capture.set(CV_CAP_PROP_POS_FRAMES, frame);
    }
    
    /**
     * Returns current frame.
     * @return current frame
     */
    public double currFrame() {
        return capture.get(CV_CAP_PROP_POS_FRAMES);
    }
    
    /**
     * Moves to a given milisecond in video.
     * @param ms ms in video to move to
     */
    public void seek(double ms) {
        capture.set(CAP_PROP_POS_MSEC, ms);
    }
    
    /**
     * Returns total frames in video file.
     * @return total frames
     */
    public double getTotalFrames() {
        return capture.get(CV_CAP_PROP_FRAME_COUNT);
    }
    
    /**
     * Returns video frame rate.
     * @return frame rate
     */
    public double getFPS() {
        return capture.get(CV_CAP_PROP_FPS);
    }
    
    /**
     * Set source file.
     * @param filename source file
     */
    public void setSource(String filename) {
        capture.release();
        capture = new VideoCapture(filename);
    }
    
    /**
     * Set source device.
     * @param sourceid device number
     */
    public void setSource(int sourceid) {
        capture.release();
        capture = new VideoCapture(sourceid);
    }
    
    /**
     * Retrieve a frame from video.
     * @param frameSkip skip similar frames if true
     * @return frame data
     */
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
                if (assignedWorker != null) {
                    assignedWorker.reportEnd();
                }
            }
        }
        
        videoSem.release();
        return frame;
    }
    
    /**
     * Retrieve a frame from video (automatically skips similar frames).
     * @return frame data
     */
    public Mat grabFrame() {
        return grabFrame(true);
    }
    
    /**
     * Converts image to byte array.
     * @param img image to convert
     * @return byte array representation of given image
     */
    public byte[] imageToByteArray(BufferedImage img) {
        byte[] bytes;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.setUseCache(false);
            ImageIO.write(img, FacerecConfig.FRAME_IMAGE_FORMAT, baos);
            baos.flush();
            
            bytes = baos.toByteArray();
            baos.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        
        //return "0".getBytes();
        return bytes;
    }
    
    
    
    //  

    /**
     * Processes a single frame, and if it is not similar to previous, sends it over assigned MQ link.
     * @param messageQueueName worker queue name
     * @param skipSimilarFrames skip similar frames if true
     * @param forceFrameNumber manually set the number of frame in the outgoing message (override actual frame number)
     * @return returns -1 for fail, 0 for ok, 1 for skipped frame
     */
    public int processFrame(String messageQueueName, boolean skipSimilarFrames, int forceFrameNumber) {
        String currFrameStr = Long.toString((long)currFrame());
        Mat frame = grabFrame();
        
        if (!frame.empty()) {
            
            if (skipSimilarFrames && forceFrameNumber==0 && !changed(frame)) {                
                // increment counter for skipped frames
                Controller.getCurrentController().getDispatcher().incrementFrameCounter();
                return 1;
            }
            
            BufferedImage bimg = matToBufferedImage(frame);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
            try {
                String header;
                if (forceFrameNumber == 0) { header = currFrameStr; }
                else { header = Integer.toString(forceFrameNumber); }
                header += ",";
                outputStream.write( header.getBytes() );
                //outputStream.write( matToByte(frame) );
                outputStream.write( imageToByteArray(bimg) );
                outputStream.flush();
            }
            catch (IOException ex) {
                return -1;
            }
            
            byte request[] = outputStream.toByteArray( );
            
            
            
            try {
                
                mqlink.publish(messageQueueName, request);
            } catch (IOException ex) {
                System.err.println("Failed to send message.");
                return -1;
            }
        }
        else {
            return -1;
        }
        return 0;
        
    }
    
    /**
     * Processes a single frame, and if it is not similar to previous, sends it over assigned MQ link.
     * @param messageQueueName worker queue name
     * @param skipSimilarFrames skip similar frames if true
     * @return returns -1 for fail, 0 for ok, 1 for skipped frame
     */
    public int processFrame(String messageQueueName, boolean skipSimilarFrames) {
        return processFrame(messageQueueName, skipSimilarFrames, 0);
    }
    
    /**
     * Processes a single frame, and if it is not similar to previous, sends it over assigned MQ link (automatically skips similar frames).
     * @param messageQueueName worker queue name
     * @return returns -1 for fail, 0 for ok, 1 for skipped frame
     */
    public int processFrame(String messageQueueName) {
        return processFrame(messageQueueName, true, 0);
    }
    
    /**
     * Grab a frame and display it in image view, also return the Image object.
     * @param imview image view to display the image in
     * @return image object
     */
    public Image displayFrame(ImageView imview) {
        
        Mat frame = grabFrame(false);
        
        if (!frame.empty()) {
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
    
    // compare with previous mat
    private boolean changed(Mat m) {
        if (lastFrameMat == null) {
            lastFrameMat = m;
            return true;
        }
        
        boolean res;
        double diff = Core.norm(lastFrameMat, m) / (m.size().area()*VideoController.MAX_IMAGE_COLOUR_VALUE);
        
        if ( diff > imgChangeThreshold) { res = true; }
        else { res = false; }
        
        lastFrameMat.release();
        lastFrameMat = m;
        return res;
    }
    
    // convert mat to BufferedImage
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
