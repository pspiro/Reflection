package testcase;

import junit.framework.TestCase;
import tw.google.GTable;
import tw.google.NewSheet;

public class TestGtable extends TestCase {
	static String aapl = "Test";
	static String tab = "Prod-symbols";
	
	public void testCsPass() throws Exception {
		GTable map = new GTable(NewSheet.Reflection, tab, "TokenSymbol", "Conid", true);
		assertEquals( "265598", map.get( "AAPL.r"));
	}

	public void testCsFail() throws Exception {
		GTable map = new GTable(NewSheet.Reflection, tab, "TokenSymbol", "Conid", true);
		assertEquals( null, map.get( "Aapl.r"));
	}
	
	public void testNonCsPass() throws Exception {
		GTable map = new GTable(NewSheet.Reflection, tab, "TokenSymbol", "Conid", false);
		assertEquals( "265598", map.get( "Aapl.R"));
	}
}
