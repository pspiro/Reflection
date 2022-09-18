/*
 * $Id: DirProcessor.java,v 1.5 2012/09/18 13:58:18 jsant Exp $
 * 
 * @author Pete 
 * @since Sep 27, 2005
 * 
 * $Log: DirProcessor.java,v $
 * Revision 1.5  2012/09/18 13:58:18  jsant
 * int bz9999 OrderStats: process files sorted by size Big to Small
 *
 * Revision 1.4  2011/08/23 20:57:04  jsant
 * int bz9999 OrderStats: double parse audit files to decide whether to store submits in the second parse
 *
 * Revision 1.3  2007/07/02 13:41:06  pspiro
 * int bz9999 check for null
 *
 * Revision 1.2  2007/06/04 17:51:10  pspiro
 * int bz9999 rearrange packages
 *
 * Revision 1.1  2007/02/14 22:49:26  linux24g
 * relocated utils
 *
 * Revision 1.1  2007/02/09 22:45:20  linux24g
 * Java files from pspiro/programs
 *
 */
package tw.grep;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

public class DirProcessor {
    private String m_wildcard;
    private boolean m_showDir;
    private boolean m_showFile;
    private FileProcessor m_preProcessor;
    private FileProcessor m_fileProcessor;
    private boolean m_recurse;
    private int m_maxFiles;
    private boolean m_sortBigToSmall; // large files processed first. so if OutOfMemory occurs it will more likely be at start than later.
    private boolean m_processDirs;
    
    public void maxFiles(int v) { m_maxFiles = v; }
    public void sortBigToSmall()    { m_sortBigToSmall = true; }
    public void processDirs( boolean v) { m_processDirs = v; }
    
    /** @param wildcard should be e.g. "*.java" */ 
    public DirProcessor( String wildcard, boolean recurse, boolean showDir, boolean showFile, FileProcessor preProcessor, FileProcessor fileProcessor) throws Exception {
        m_wildcard = wildcard;
        m_recurse = recurse;
        m_showDir = showDir;
        m_showFile = showFile;
        m_preProcessor = preProcessor;
        m_fileProcessor = fileProcessor;
    }
    
    public DirProcessor( String wildcard, boolean recurse, boolean showDir, boolean showFile, FileProcessor fileProcessor) throws Exception {
        this(wildcard, recurse, showDir, showFile, null, fileProcessor);
    }
    
    public void process( String dirName) {
    	process( new File( dirName) );
    }
    
    public void process( File dir) {
        FileFilter filter = new FileFilter(){
            public boolean accept(File pathname) {
                if( pathname.isDirectory() ) {
                    return m_recurse || m_processDirs;
                }
                return matches( pathname.getName().toUpperCase(), m_wildcard.toUpperCase() );
            }
        };

        File[] files = dir.listFiles(filter);
        if (m_sortBigToSmall) {
            files = sortBySizeBigToSmall(files);
        }

        if( files != null) {
            for( int a = 0; a < files.length && (a < m_maxFiles || m_maxFiles == 0); a++) {
                File file = files[a];
                if( file.isDirectory() ) {
                    if( m_showDir) {
                        System.out.println( "Processing directory " + file.getAbsolutePath() );
                    }
                    if (m_processDirs) {
                    	m_fileProcessor.process( file);
                    }
                    if (m_recurse) {
                    	process( file.getAbsolutePath() );
                    }
                }
                else {
                    if( m_showFile) {
                        System.out.println( "Processing file " + file.getAbsolutePath() );
                    }
                    if (m_preProcessor != null) {
                        m_preProcessor.process(file);
                    }
                    m_fileProcessor.process( file);
                }
            }
        }
    }
    
    private static File[] sortBySizeBigToSmall(File[] files) {
        List<File> list = new ArrayList<File>(Arrays.asList(files));
        Collections.sort(list, new Comparator<File>() {
            public int compare(File o1, File o2) {
                long l1 = o1.length();
                long l2 = o2.length();
                return (l2 < l1 ? -1 : (l2 == l1 ? 0 : 1));
            }
        });
        return list.toArray(new File[list.size()]);
    }
    
    // move to util
    static boolean matches(String name, String wildcard) {
        int pos = 0;
        
        StringTokenizer st = new StringTokenizer( wildcard, "*");

        if( wildcard.charAt( 0) != '*') {
            String firstTok = st.nextToken();
            if( name.length() < firstTok.length() || !name.substring( 0, firstTok.length() ).equals( firstTok) ) {
                return false;
            }
            pos = firstTok.length();
        }
        
        while( st.hasMoreTokens() ) {
            String tok = st.nextToken();
            int index = name.indexOf( tok, pos);
            if( index == -1) {
                return false;
            }
            pos = index + tok.length();
        }
        return pos == name.length() || wildcard.charAt( wildcard.length() - 1) == '*';
    }
}