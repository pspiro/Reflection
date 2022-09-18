/*
 * $Id: FileProcessor.java,v 1.2 2007/06/04 17:51:10 pspiro Exp $
 * 
 * @author Pete 
 * @since Sep 27, 2005
 * 
 * $Log: FileProcessor.java,v $
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

public interface FileProcessor {
    void process(File file);
}
