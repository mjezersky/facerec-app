/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facerec.flow;

import java.util.Objects;

/**
 *
 * @author ASUS
 */
public class ListElementFlow extends Flow {
    
    public Occurrence person;
    
    public ListElementFlow(Flow f) {
        this.age = f.age;
        this.firstFrame = f.firstFrame;
        this.lastFrame = f.lastFrame;
        this.lastRectangle = f.lastRectangle;
        this.person = f.mostFrequentOccurrence();
        
    }
    
    
    @Override
    public boolean equals(Object o) {
        if (o == null) { return false; }
        if (o.getClass() != ListElementFlow.class) { return false; }
        ListElementFlow lef = (ListElementFlow) o;
        
        boolean res = this.firstFrame == lef.firstFrame &&
                this.lastFrame == this.firstFrame &&
                this.person.equals(lef.person);
        
        return res;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.person);
        hash = 17 * hash + (int) (Double.doubleToLongBits(this.firstFrame) ^ (Double.doubleToLongBits(this.firstFrame) >>> 32));
        hash = 17 * hash + (int) (Double.doubleToLongBits(this.lastFrame) ^ (Double.doubleToLongBits(this.lastFrame) >>> 32));
        return hash;
    }
    
    @Override
    public String toString() {
        String res = "";

        /*res += Long.toString((long) firstFrame)+",";
        res += Long.toString((long) lastFrame)+",";
        res += person.toString();*/
        res = person.name + " (" +  String.format("%.2f", person.bestConfidence) + ")";

        return res;
    }
}
