/*
 * $Id: LineProcessor.java,v 1.1 2007/02/14 22:49:27 linux24g Exp $
 * 
 * @author Pete 
 * @since Oct 25, 2005
 * 
 * $Log: LineProcessor.java,v $
 * Revision 1.1  2007/02/14 22:49:27  linux24g
 * relocated utils
 *
 * Revision 1.1  2007/02/09 22:45:23  linux24g
 * Java files from pspiro/programs
 *
 */
package tw.grep;

import java.io.File;

public interface LineProcessor {
    /** @return false if you want to stop processing */
    public boolean process(File file, String line, int lineNum);
}
