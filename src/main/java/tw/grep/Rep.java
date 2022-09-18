package tw.grep;

import java.io.File;

import tw.util.OStream;
import tw.util.Std;

public class Rep {
    //public static boolean s_whole = true;
    
    public static void main(String[] args) {
        try {
            if( args.length < 3) {
                System.out.println( "usage: [-u] [-r] rep find replace wildcard");
                System.out.println( "wildcards must be enclosed in quotation marks");
                System.out.println( "-u: use unix eol characters");
                System.out.println( "-r: recursive");
                return;
            }
            
            int argCounter = 0;
            boolean recurse = false;
            
            // parse flags
            while( args[argCounter].charAt( 0) == '-') {
                String str = args[argCounter++];
                //if( str.equals( "-p") ) {
                //    s_whole = false;
                //}
                if( str.equals( "-r") ) {
                    recurse = true;
                    Std.err( "Recursive");
                }
                if( str.equals( "-u") ) {
                    OStream.setUnixEol();
                    Std.err( "Unix EOL");
                }
            }

            String find = args[argCounter++];
            String replace = args[argCounter++];
            String filenameOrWildcard = args[argCounter++];

            Std.err( "Find: " + find);
            Std.err( "Replace: " + replace);
            Std.err( "Filename: " + filenameOrWildcard);

            boolean hasWildcard = filenameOrWildcard.indexOf( '*') != -1;
            
            LineFixer lineFixer = new TextReplacer( find, replace);
        
            FileProcessor fileProcessor = new FileFixer( lineFixer);
            
            if( hasWildcard) {
                Std.err( "Processing with wildcard");
                DirProcessor processor = new DirProcessor(filenameOrWildcard, recurse, false, false, fileProcessor);
                processor.process(".");
            }
            else {
                Std.err( "Processing single file");
                File file = new File( filenameOrWildcard);
                fileProcessor.process( file);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    /** Used for testing. */
    static String fix(String find, String replace, String string) {
        StringBuffer buf = new StringBuffer( string);
        fix( find, replace, buf);
        return buf.toString();
    }
    
    /** Called by TextReplacer. */
    public static boolean fix(String find, String replace, StringBuffer buf) {
        boolean fixed = false;
        int i = indexOf( buf, find, 0);
        while( i != -1) {

            boolean firstOk = i == 0 || !Grep.alnum( buf.charAt( i - 1) );

            int nextCharPos = i + find.length();
            boolean lastOk = nextCharPos == buf.length() || !Grep.alnum( buf.charAt( nextCharPos) );

            if( firstOk && lastOk) {
                buf.replace( i, i + find.length(), replace);
                Std.err( buf.toString() );
                fixed = true;
            }

            i = buf.indexOf( find, i + find.length() );
        }
        return fixed;
    }

    /** A "*" in the find string will match any single character in the buf. */
    private static int indexOf( StringBuffer buf, String find, int start) {
        for( int a = start; a <= buf.length() - find.length(); a++) {
            boolean found = true;
            
            for( int i = 0; i < find.length(); i++) {
                if( !match( buf.charAt( a+i), find.charAt( i) ) ) {
                    found = false;
                    break;
                }
            }
            
            if( found) {
                return a;
            }
        }

        return -1;
    }

    private static boolean match( char c1, char c2) {
        return c1 == c2 || c2 == '*';
    }

}
