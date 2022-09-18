/*
 * $Id: Grep.java,v 1.5 2011/10/21 13:22:13 pspiro Exp $
 *
 * @author Pete
 * @since Sep 27, 2005
 *
 * $Log: Grep.java,v $
 * Revision 1.5  2011/10/21 13:22:13  pspiro
 * int bz9999 no output
 *
 * Revision 1.4  2010/11/16 22:41:22  pspiro
 * int bz9999 no line nums
 *
 * Revision 1.3  2009/04/30 13:03:45  ptitov
 * int dp0000 fix common alias that breaks builds
 *
 * Revision 1.2  2008/08/08 15:11:43  pspiro
 * int bz9999 update parse
 *
 * Revision 1.1  2007/02/14 22:49:26  linux24g
 * relocated utils
 *
 * Revision 1.1  2007/02/09 22:45:21  linux24g
 * Java files from pspiro/programs
 *
 */
package tw.grep;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

import tw.util.Std;

public class Grep {
    private static String AND = " AND ";
    public static boolean s_whole = true;
    public static boolean s_showLineNums = false;
    
    private static void usage() {
        System.out.println( "usage: grep [-p partial match] [-r recursive] [-ln line#'s] find filename or wildcard");
        System.out.println( "wildcards must be enclosed in quotation marks");
    }

    public static void main(String[] args) {

        if( args.length > 4 || args.length < 2) {
            usage();
            return;
        }

        try {
            int argCounter = 0;
            boolean recurse = false;
            
            // parse flags
            while( args[argCounter].charAt( 0) == '-') {
                String str = args[argCounter++];
                if( str.equals( "-p") ) {
                    s_whole = false;
                }
                if( str.equals( "-r") ) {
                    recurse = true;
                }
                if( str.equals( "-ln") ) {
                    s_showLineNums = true;
                }
            }
            
            String find = args[argCounter++];
            String filenameOrWildcard = args[argCounter++];
            boolean hasWildcard = filenameOrWildcard.indexOf( '*') != -1;
            
            
            // get list of strings separated by AND
            final String[] finds = getStrings( find);
            
            // create file processor
            LineFileProcessor fileProcessor = new LineFileProcessor( new LineProcessor() {
                public boolean process(File file, String line, int lineNum) {
                    search( finds, file, line, lineNum);
                    return true;
                }
            });
    
            // wildcard specified?
            if( hasWildcard || recurse) {
                DirProcessor processor = new DirProcessor(filenameOrWildcard, recurse, false, false, fileProcessor); 
                processor.process(".");
            }
            
            // no wildcard specified
            else {
                File file = new File( filenameOrWildcard); 
                fileProcessor.process( file);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean search(String[] finds, File file, String line, int lineNum) {
        for( int a = 0; a < finds.length; a++) {
            if( !lineHas( line, finds[a]) ) {
                return true;
            }
        }
        
        if( s_showLineNums) {
            System.out.println( file.getAbsolutePath() + ": " + lineNum + " " + line);
        }
        else {
            System.out.println( line);
        }
        
        return false;
    }
    
    public static boolean lineHas( String line, String find) {
        
        int i = line.indexOf( find);
        if( i == -1) {
            return false;
        }
        
        if( !s_whole) {
            return true;
        }

        boolean firstOk = i == 0 || !alnum( line.charAt( i - 1) ) || !alnum( line.charAt( i) );
        
        int nextCharPos = i + find.length();
        boolean lastOk = nextCharPos == line.length() || !alnum( line.charAt( nextCharPos) ) || !alnum( line.charAt( nextCharPos - 1) );
        
        return firstOk && lastOk;
    }
    
    /** Get list of strings separated by AND. */
    public static String[] getStrings( String find) {
        ArrayList list = new ArrayList();
    
        while( true) {
            int i = find.indexOf( AND);
            if( i == -1) {
                list.add( find);
                break;
            }
            
            String str = find.substring( 0, i);
            list.add( str);
            Std.err( "find " + str);
            
            find = find.substring( i + AND.length() );
        }
        
        return (String[])list.toArray( new String[list.size()]);
    }

    static boolean alnum(char c) {
        return c >= 'a' && c <= 'z' ||
               c >= 'A' && c <= 'Z' ||
               c >= '0' && c <= '9' ||
               c == '_';
    }
    public static boolean copyFile( String source, String dest) {
        // this function copies a file

        try {
            // open input and output files
            FileInputStream input = new FileInputStream( source);
            FileOutputStream output = new FileOutputStream( dest);

            // loop through the bytes in the input file
            byte[] buf = new byte[1024];
            while( true) {
                int bytes = input.read( buf);
                if( bytes == 0 || bytes == -1) {
                    break;
                }
                output.write( buf, 0, bytes);
            }

            // close files
            input.close();
            output.close();
            return true;
        }
        catch( Exception e) {
            System.out.println( "Copy file from " + source + " to " + dest + " failed");
            return false;
        }
    }

}
