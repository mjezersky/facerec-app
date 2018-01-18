
package facerec;

import java.io.StringWriter;


public class RectangleObject {
    public int left = 0;
    public int top = 0;
    public int right = 0;
    public int bottom = 0;


    public void scale(double factor) {
        left *= factor;
        top *= factor;
        right *= factor;
        bottom *= factor;
    }

    public static RectangleObject deserializeRect(String msgString) {
        // Deserialization FSM
        
        if (!msgString.contains("$")) {
            return null;
        }
        
        int state = 0;
        RectangleObject rect = new RectangleObject();
        StringWriter sw = new StringWriter();
        
        boolean terminate = false;
        for (int strInd = 1; strInd < msgString.length(); strInd++) {
            char cc = msgString.charAt(strInd);
            if (terminate) { break; }
            if (cc == ' ') { continue; }
            
            switch (state) {
                case 0:
                    if (cc == ',') {
                        rect.left = Integer.parseInt(sw.toString());
                        sw = new StringWriter();
                        state = 1;
                        break;
                    }
                    sw.append(cc);
                    break;
                case 1:
                    if (cc == ',') {
                        rect.top = Integer.parseInt(sw.toString());
                        sw = new StringWriter();
                        state = 2;
                        break;
                    }
                    sw.append(cc);
                    break;
                case 2:
                    if (cc == ',') {
                        rect.right = Integer.parseInt(sw.toString());
                        sw = new StringWriter();
                        state = 3;
                        break;
                    }
                    sw.append(cc);
                    break;
                case 3:
                    if (cc == ']') {
                        rect.bottom = Integer.parseInt(sw.toString());
                        terminate = true;
                        break;
                    }
                    sw.append(cc);
                    break;
            }
        }
        return rect;
    }
}
