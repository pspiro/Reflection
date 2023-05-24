package testcase;

import reflection.Util;
import tw.util.S;

public class TestStrings extends MyTestCase {
	public void testFormat() {
		S.out( "%s %s %s", 2.0, 2.22, 2.222);
	}

	public void testFormat2() {
		S.out( "ab%cd", new Object[0]);
	}
	
	public void testLeftAndRight() {
		S.out( "ab=" + Util.left("abc", 2) );
		S.out( "abc=" + Util.left("abc", 5) );
		S.out( "bc=" + Util.right("abc", 2) );
		S.out( "abc=" + Util.right("abc", 5) );
	}
}
