/*
 * $Id: TextReplacer.java,v 1.1 2007/02/14 22:49:27 linux24g Exp $
 * 
 * @author Pete 
 * @since Dec 9, 2005
 * 
 * $Log: TextReplacer.java,v $
 * Revision 1.1  2007/02/14 22:49:27  linux24g
 * relocated utils
 *
 * Revision 1.1  2007/02/09 22:45:25  linux24g
 * Java files from pspiro/programs
 *
 */
package tw.grep;

public class TextReplacer implements LineFixer {
    private String m_find;
    private String m_replace;

    public TextReplacer( String find, String replace) {
        m_find = find;
        m_replace = replace;
    }
    
    public boolean fix( String filename, StringBuffer line, int num) {
        return Rep.fix( m_find, m_replace, line);
    }
}
