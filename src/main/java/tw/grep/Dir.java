package tw.grep;

import java.io.File;

import tw.util.Std;

public class Dir {
    public static void main(String[] args) {
        try {
            System.out.println("char(127)=" + ((char) 127));
            
            DirProcessor dp = new DirProcessor( args[0], false, false, false, new FileProcessor() {
                public void process(File file) {
                    Std.err( "" + file);
                }
            });
            dp.process( ".");
        }
        catch( Exception e) {
            e.printStackTrace();
        }
    }
}
