
package facerec.flow;

import facerec.RectangleObject;
import facerec.result.Result;
import facerec.result.ResultFragment;
import java.util.ArrayList;
import java.util.Objects;


public class FlowRecorder {
    
    private ArrayList<Flow> buffer;
    private int maxAge = 3;                 // max flow age
    private double matchThreshold = 0.8;
    
    public FlowRecorder() {
        this.buffer = new ArrayList();
    }
    
    public void feed(Result res) {
        
        
        for (ResultFragment fmt : res.fragments) {
            RectangleObject r = RectangleObject.deserializeRect(fmt.rectString);
            String person = fmt.name;
            boolean match = false;
            
            for (Flow f : buffer) {
                if (f.match(r, matchThreshold)) {
                    f.feed(res.frame, person, r);
                    match = true;
                    break;
                }
            }
            
            // if no match, create new flow
            if (!match) {
                buffer.add(new Flow(res.frame, r));
            }
            
        }
        
        // age all flows
        for (Flow f : buffer) {
            f.age++;
            if (f.age>maxAge) {
                // finalize old flows
                buffer.remove(f);
                f.flush();
            }
        }

    }
    
    public void flush() {
        // finalize all remaining flows
        for (Flow f : buffer) {
            f.flush();
        }
        buffer.clear();
    }
    
    private class Occurrence {
        public String person;
        public int count;
        
        public Occurrence(String person) {
            this.person = person;
        }
        
        public void inc() {
            count++;
        }
        
        @Override
        public boolean equals(Object o) {
            if (o == null) { return false; }
            if (o.getClass() != Occurrence.class) { return false; }
            return this.person.equals( ((Occurrence) o).person );
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 29 * hash + Objects.hashCode(this.person);
            return hash;
        }
        
    }
    
    private class Flow {
        public int age = 0;
        public double firstFrame;
        public double lastFrame;
        public RectangleObject lastRectangle;
        public ArrayList<Occurrence> occurrences;
        
        public Flow(double frameNum, RectangleObject rectangle) {
            this.occurrences = new ArrayList();
            this.firstFrame = this.lastFrame = frameNum;
            this.lastRectangle = rectangle;
        }
        
        public void feed(double frameNum, String person, RectangleObject rectangle) {
            age = 0;
            lastFrame = frameNum;
            lastRectangle = rectangle;
            
            // find if person exists in occurrences and increment count, or create a new one
            for (Occurrence oc : occurrences) {
                if (oc.person.equals(person)) {
                    oc.count++;
                    return;
                }
            }
            // if program reached here, no occurrence was found, create a new one
            occurrences.add(new Occurrence(person));
            
        }
        
        public boolean match(RectangleObject r, double threshold) {
            return this.lastRectangle.similar(r, threshold);
        }
        
        public void flush() {
            // TODO
        }
        
        @Override
        public boolean equals(Object o) {
            if (o == null) { return false; }
            if (o.getClass() != Flow.class) { return false; }
                        
            return (this.lastRectangle.equals( ((Flow) o).lastRectangle ));
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 97 * hash + Objects.hashCode(this.lastRectangle);
            return hash;
        }
        
    }
}
