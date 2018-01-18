
package facerec;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Link {

    private Socket serverSocket;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter out;
    private String currentLabel = "";
    boolean trainingMode = false;
    
    public void setTrainingMode(boolean mode) { this.trainingMode = mode; }
    
    public void setTrainLabel(String label) { this.currentLabel = label; }
    
    public void connect(String host, int port) {
        try {
            System.out.println("connecting to "+host+":"+String.valueOf(port));
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("done...");
        } catch (IOException ex) {
            
        }
        
        
    }
    
    public void close() {
        if (socket == null) { return; }
        try { socket.close(); }
        catch (IOException ex) {}
    }
    
    public String recvData() {
        if (reader == null) { return "ERROR RETRIEVING RESPONSE"; }
        String lenStr = "";
        StringWriter sw = new StringWriter();
        int c;
        try {
            while (true) {
                    c=reader.read();
                    if (((char) c)=='#') {
                        break;
                    }
                    sw.append((char) c);
            }
            lenStr = sw.toString();
            
            
            sw = new StringWriter();
            for (int i=0; i<Integer.parseInt(lenStr); i++) {
                sw.append((char) reader.read());
            }
            
            DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
            dataOut.writeBytes("ACK");
            
            return sw.toString();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }
   
    
    public String processRequest(byte[] msg) {

        if (trainingMode) {
            communicate("STORE".getBytes());
            communicate(this.currentLabel.getBytes());
        }
        else {
            communicate("RECOG".getBytes());
        }
        
        
        return communicate(msg);
    }
    
    public String sendCommand(String msg) {
        return communicate(msg.getBytes());
    }
    
    public String dumpDB() {
        communicate("DUMPDB".getBytes());
        return recvData();
    }
    
    public String communicate(byte[] msg) {
        send(msg);
        //send(msg);
        return recvData();
    }
    
    public boolean send(byte[] msg) {
        if (socket == null) { return false; }
        if (!socket.isConnected()) { return false; }
        try {
            DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
            dataOut.write((String.valueOf(msg.length)+"#").getBytes());
            dataOut.write(msg);
            dataOut.flush();
            
            // ack
            for (int i=0; i<3; i++) {
                reader.read();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }
    
}
