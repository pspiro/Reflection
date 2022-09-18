/*
 * $Id: LineFixer.java,v 1.1 2007/02/14 22:49:26 linux24g Exp $
 * 
 * @author Pete 
 * @since Nov 9, 2005
 * 
 * $Log: LineFixer.java,v $
 * Revision 1.1  2007/02/14 22:49:26  linux24g
 * relocated utils
 *
 * Revision 1.1  2007/02/09 22:45:22  linux24g
 * Java files from pspiro/programs
 *
 */
package tw.grep;

/** Used with FileFixer. */
public interface LineFixer {
    /** @return true if the line was fixed */
    public boolean fix( String filename, StringBuffer line, int num);
}
