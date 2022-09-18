/*
 * $Id: ArString.java,v 1.2 2010/10/19 20:37:02 pspiro Exp $
 * 
 * @author Pete 
 * @since Sep 28, 2005
 * 
 * $Log: ArString.java,v $
 * Revision 1.2  2010/10/19 20:37:02  pspiro
 * int bz9999 parse
 *
 * Revision 1.1  2007/02/14 22:49:30  linux24g
 * relocated utils
 *
 * Revision 1.1  2007/02/09 22:45:59  linux24g
 * Java files from pspiro/programs
 *
 */
package tw.util;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class ArString extends ArrayList<String> {
    public String gett( int i) {
        return get( i);
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        for( int a = 0; a < size(); a++) {
            if( a > 0) {
                buf.append( ", ");
            }
            buf.append( get( a) );
        }
        return buf.toString();
    }
    
    public void setFrom( String str, String sep) {
        StringTokenizer st = new StringTokenizer( str, sep);
        while( st.hasMoreTokens() ) {
            add( st.nextToken() );
        }
    }
}

