/*
 * $Id: Profiler.java,v 1.5 2013/03/12 14:01:29 zsajti Exp $
 * 
 * @author Attila Doroszlai
 * @since Aug 26, 2011
 * 
 * $Log: Profiler.java,v $
 * Revision 1.5  2013/03/12 14:01:29  zsajti
 * ext dp17624 Chart contract selection improvement (first part)
 *
 * Revision 1.4  2013/02/27 10:20:10  zsajti
 * ext dp17609 Chart vertical scrollbar adjustment
 *
 * Revision 1.3  2013/02/07 10:42:32  zsajti
 * ext dp17543 Line chart equivalency
 *
 * Revision 1.2  2012/06/01 14:07:00  adoroszl
 * int bz0000 Fix build error
 *
 * Revision 1.1  2012/05/31 13:54:34  adoroszl
 * int bz0000 Simple profiler class
 *
 */
package tw.util;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/** Simple profiler.  It is not reentrant, ie. recursive calls are not supported. */
public class Profiler {
    
    private static final Profiler INSTANCE = new Profiler();
    private final Map<String, ProfiledItem> m_map = new TreeMap<String, ProfiledItem>();
    
    public static Profiler instance() { return INSTANCE; }
    
    private Profiler() {
        // singleton
    }
    
    public void trace(String key) {
        getOrCreateItem(key).calledFrom(String.valueOf(caller(1)));
    }

    public void start(String key) {
        getOrCreateItem(key).start();
    }

    private ProfiledItem getOrCreateItem(String key) {
        ProfiledItem item = m_map.get(key);
        if (item == null) {
            item = new ProfiledItem();
            m_map.put(key, item);
        }
        return item;
    }
    
    public void end(String key) {
        m_map.get(key).end();
    }
    
    public void dump() {
        for (String key : m_map.keySet()) {
            ProfiledItem profiledItem = m_map.get(key);
            if (profiledItem.m_count > 0) {
                if (profiledItem.m_start > 0) {
                    profiledItem.end();
                }
                double avg = profiledItem.m_elapsedNanos / 1000.0 / profiledItem.m_count;
                System.out.println("PROF " + key + " count: " + profiledItem.m_count + ", elapsed: " + (profiledItem.m_elapsedNanos/1000) + " us, avg: " + avg + " us/call");
            }
            if (!profiledItem.m_callCounts.isEmpty()) {
                System.out.println("PROF " + key + " calls: " + profiledItem.m_callCounts);
            }
            profiledItem.reset();
        }
    }
    
    private static class ProfiledItem {
        
        private final Map<String, Integer> m_callCounts = new HashMap<String, Integer>();
        private int m_count;
        private long m_elapsedNanos;
        private long m_start;
        
        public void calledFrom(String caller) {
            if (m_callCounts.containsKey(caller)) {
                m_callCounts.put(caller, m_callCounts.get(caller) + 1);
            } else {
                m_callCounts.put(caller, 1);
            }
        }
        
        public void reset() {
            m_count = 0;
            m_elapsedNanos = 0L;
            m_start = 0L;
            m_callCounts.clear();
        }
        
        public void start() {
            if (m_start > 0) {
                throw new IllegalStateException("profiler is not reentrant");
            }
            m_start = System.nanoTime();
        }

        public void end() {
            if (m_start == 0) {
                throw new IllegalStateException("profiler not started");
            }
            long end = System.nanoTime();
            m_elapsedNanos += (end - m_start);
            ++m_count;
            m_start = 0;
        }
        
    }
    
    public static StackTraceElement caller() { return caller(1); /* our caller is one call deeper by now */ }
    public static StackTraceElement caller(int depth) { 
        StackTraceElement[] caller = caller(++depth, 1);
        return caller == null || caller.length == 0 ? null : caller[0]; /* our caller is one call deeper by now */ 
   }
    
    public static StackTraceElement[] caller(int stackDepth, int length) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        stackDepth += 3; // we want the caller of our caller +1, we are deeper by one call +1, and getStackTrace is another call above us +1
        return stackTrace != null && stackTrace.length > stackDepth 
            ? Arrays.copyOfRange(stackTrace, stackDepth, Math.min(stackDepth + length, stackTrace.length)) 
            : null;
    }
    
    public static <T> void dump(PrintStream printer, T ... elements) {
        if (elements != null) {
            for(T element : elements) {
                printer.println(element);
            }
        } else {
            printer.println(elements);
        }
    }
    
    public static void dumpTrace(PrintStream printer) { dump(printer, Thread.currentThread().getStackTrace()); }
    public static void dumpTrace() { dumpTrace(System.out); }
}
