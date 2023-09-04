package reflection;

import common.Util;
import tw.google.NewSheet;
import tw.util.S;

public class BackupConfig {
	public static void main(String[] args) throws Exception {
		NewSheet.getBook(NewSheet.Reflection).save("c:/sync/reflection/archive/" + Util.uid(6) );
		S.out("Done");
	}
	
}
