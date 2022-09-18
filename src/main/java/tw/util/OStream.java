/*
 * $Id: OStream.java,v 1.1 2007/02/14 22:49:31 linux24g Exp $
 * 
 * @author Pete 
 * @since Sep 27, 2005
 * 
 * $Log: OStream.java,v $
 * Revision 1.1  2007/02/14 22:49:31  linux24g
 * relocated utils
 *
 * Revision 1.1  2007/02/09 22:46:00  linux24g
 * Java files from pspiro/programs
 *
 */
package tw.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class OStream extends FileOutputStream {
    static String WINDOWS = "\r\n";
    static String UNIX = "\n";
    static String s_eol = WINDOWS;
    
    public static void setUnixEol() {
        s_eol = UNIX;
    }

    public OStream(String name) throws FileNotFoundException {
        super(name);
    }
    
    public OStream(String name, boolean append) throws FileNotFoundException {
        super(name, append);
    }
    
    
    
    
	public static OStream create( String name, boolean append) {
		try {
			return new OStream( name, append);
		} 
		catch (FileNotFoundException e) {
			System.err.println( e.toString() );
			return null;
		}
	}

	public void log( String str) {
		writeln( Thread.currentThread().getName() + "\t" + S.now() + "\t" + str);
	}

	public void log2( String str) {
		writeln( S.now() + "," + str);
	}

	public void reportln( Object... fields) {
		report( fields);
		writeln(); 
	}

	/** Write comma-delimted fields. */
	public void report( Object... fields) {
		write( S.concat( fields) ); 
	}

	@Override public void close() {
		try {
			super.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void write( String line) {
		try {
			write( line.getBytes() );
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    public void writeln( String format, Object... vals) {
    	writeln( String.format( format, vals) );
    }

    public void writeln( String str) {
		try {
	        write( str.getBytes() );
	        write( s_eol);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
    }
	
	public void writeln() {
		writeln( "");
	}
}
