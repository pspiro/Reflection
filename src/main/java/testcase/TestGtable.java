package testcase;

import junit.framework.TestCase;
import tw.google.GTable;
import tw.google.NewSheet;

public class TestGtable extends TestCase {
	static String qqq = "Test";
	static String tab = "Dev-symbols";
	
	public void testTable() throws Exception {
		GTable map = new GTable(NewSheet.Reflection, tab, "TokenAddress", "Symbol");
		String val = map.get( "0x97F0D430Ed153986D9f3Fa8C5Cff9b45c3e6a9Ad");
		assertEquals( qqq, val);
	}

	public void testTable2() throws Exception {
		GTable map = new GTable(NewSheet.Reflection, tab, "TokenAddress", "Symbol", false);
		String val = map.get( "0x97F0D430Ed153986D9f3Fa8C5Cff9b45c3e6a9Ad");
		assertEquals( qqq, val);
	}

	public void testTable3() throws Exception {
		GTable map = new GTable(NewSheet.Reflection, tab, "TokenAddress", "Symbol");
		String val = map.get( "0x97f0d430ed153986d9f3fa8c5cff9b45c3e6a9ad");
		assertEquals( null, val);
	}

	public void testTable4() throws Exception {
		GTable map = new GTable(NewSheet.Reflection, tab, "TokenAddress", "Symbol", false);
		String val = map.get( "0x97f0d430ed153986d9f3fa8c5cff9b45c3e6a9ad");
		assertEquals( qqq, val);
	}
}
