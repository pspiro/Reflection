/*
* $Id: BufStream.java,v 1.1 2012/11/01 21:35:24 pspiro Exp $
* 
* $Log: BufStream.java,v $
* Revision 1.1  2012/11/01 21:35:24  pspiro
* int bz9999 BufStream
*
*/

package tw.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class BufStream {
    static String WINDOWS = "\r\n";
    static String UNIX = "\n";
    static String s_eol = WINDOWS;
    
    private FileWriter os;
    private BufferedWriter bf;
    
    public static void setUnixEol() {
        s_eol = UNIX;
    }

    public BufStream(String name) throws IOException {
        this( name, false);
    }
    
    public BufStream(String name, boolean append) throws IOException {
        os = new FileWriter( name, append);
        bf = new BufferedWriter( os);
    }
    
    public void write( String str) throws Exception {
        bf.write( str);
    }

    public void writeLn( String str) throws Exception {
        bf.write( str);
        bf.write( s_eol);
    }

    public void flush() throws IOException {
        bf.flush();
    }
}
