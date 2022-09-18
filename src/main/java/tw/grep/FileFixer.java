/*
 * $Id: FileFixer.java,v 1.2 2007/06/04 17:51:10 pspiro Exp $
 * 
 * @author Pete 
 * @since Nov 8, 2005
 * 
 * $Log: FileFixer.java,v $
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import tw.util.OStream;

public class FileFixer implements FileProcessor {
    private LineFixer m_lineFixer;

    public FileFixer( LineFixer lineFixer) {
        m_lineFixer = lineFixer;
    }
    
    public void process(File inFile) {
        try {
            process2(inFile);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void process2( File inFile) throws Exception {
        String inName = inFile.getAbsolutePath();
        String outName = inName + ".out";
        OStream oStream = new OStream( outName);
        
        boolean someFixed = false;
        
        BufferedReader reader = new BufferedReader(new FileReader(inFile));
        int i = 0;
        for (String line = reader.readLine(); line != null; line = reader.readLine() ) {
            StringBuffer buf = new StringBuffer( line);
            if( m_lineFixer.fix( inFile.getName(), buf, i++) ) {
                someFixed = true;
            }
            oStream.writeln( buf.toString() );            
        }

        reader.close();
        oStream.close();
        
        File outFile = new File( outName);
        if( someFixed) {
            inFile.delete();
            outFile.renameTo( inFile);
        }
        else {
            outFile.delete();
        }
    }

}
