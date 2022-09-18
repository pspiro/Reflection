package tw.grep;

import tw.util.OStream;


public class Fix {
	public static void main(String[] args) {
		OStream.setUnixEol();
		
        LineFixer lineFixer = new LineFixer() {
			@Override public boolean fix(String filename, StringBuffer line, int num) {
			    return true;
			}
        };
        
        FileProcessor fp = new FileFixer( lineFixer);

		try {
			DirProcessor p = new DirProcessor( "*.java", true, false, true, fp);
			p.process("C:\\Users\\Peter\\tws-api");
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}
}
