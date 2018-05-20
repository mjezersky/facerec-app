
package facerec;

/**
 * Class used for performance testing during debugging.
 * @author Matous Jezersky
 */
public class Perf {
    private static long staticClock = 0;
    private long clock = 0;
    
    
    public static void staticStart() {
        staticClock = System.currentTimeMillis();
    }
    
    public static long staticStop() {
        return System.currentTimeMillis()-staticClock;
    }
    
    public void start() {
        clock = System.currentTimeMillis();
    }
    
    public long stop() {
        return System.currentTimeMillis()-clock;
    }

}
