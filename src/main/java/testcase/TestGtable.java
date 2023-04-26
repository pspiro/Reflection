package testcase;

import junit.framework.TestCase;
import tw.google.GTable;
import tw.google.NewSheet;

public class TestGtable extends TestCase {
	static String aapl = "Test";
	static String tab = "Master-symbols";
	
	public void testTable1() throws Exception {
		GTable map = new GTable(NewSheet.Reflection, tab, "TokenSymbol", "Description", true);
		assertEquals( "Apple Inc.", map.get( "AAPL"));
	}

	public void testTable2() throws Exception {
		GTable map = new GTable(NewSheet.Reflection, tab, "TokenSymbol", "Description", true);
		assertEquals( null, map.get( "Aapl"));
	}
	
	public void testTable3() throws Exception {
		GTable map = new GTable(NewSheet.Reflection, tab, "TokenSymbol", "Description", false);
		assertEquals( "Apple Inc.", map.get( "Aapl"));
	}
}
