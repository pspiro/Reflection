/*
 * $Id: FixMerge.java,v 1.1 2007/02/14 22:49:26 linux24g Exp $
 * 
 * @author Peter Spiro
 * @since Nov 10, 2006
 * 
 * $Log: FixMerge.java,v $
 * Revision 1.1  2007/02/14 22:49:26  linux24g
 * relocated utils
 *
 * Revision 1.1  2007/02/09 22:45:21  linux24g
 * Java files from pspiro/programs
 *
 */
package tw.grep;

public class FixMerge {
    public static void main(String[] args) {
        try {
            LineFixer lineFixer = new LineFixer() {
                public boolean fix(String filename, StringBuffer line, int num) {
                    return onFix( filename, line);
                }
            };
        
            FileProcessor fileProcessor = new FileFixer( lineFixer);
            
            DirProcessor processor = new DirProcessor( "*.java", true, true, false, fileProcessor);
            processor.process("e:\\SingleProjectWorkspace\\JtsConnection\\source");
            processor.process("e:\\SingleProjectWorkspace\\JtsConnection\\chart");
            
            System.out.println( "done");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        
    }

    protected static boolean onFix(String filename, StringBuffer line) {
        if( line.length() >= 7) {
            String sub = line.substring( 0, 7);
            
            if( sub.equals( "<<<<<<<") ||
                sub.equals( "=======") ||
                sub.equals( ">>>>>>>") ) {
                
                line.setLength( 0);
                line.append( " *");
                return true;
            }
        }
        return false;
    }
}
