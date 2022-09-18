/*
 * $Id: LineFileProcessor.java,v 1.2 2007/06/04 17:51:10 pspiro Exp $
 * 
 * @author Pete 
 * @since Oct 25, 2005
 * 
 * $Log: LineFileProcessor.java,v $
 * Revision 1.2  2007/06/04 17:51:10  pspiro
 * int bz9999 rearrange packages
 *
 * Revision 1.1  2007/02/14 22:49:26  linux24g
 * relocated utils
 *
 * Revision 1.1  2007/02/09 22:45:22  linux24g
 * Java files from pspiro/programs
 *
 */
package tw.grep;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class LineFileProcessor implements FileProcessor {
    private LineProcessor m_lineProcessor;
    
    public LineFileProcessor( LineProcessor p) {
        m_lineProcessor = p;
    }
    
    public void process(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));

            int lineNum = 1;
            for (String line = reader.readLine(); line != null; line = reader.readLine(), lineNum++) {
                if (!m_lineProcessor.process(file, line, lineNum)) {
                    break;
                }
            }

            reader.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
