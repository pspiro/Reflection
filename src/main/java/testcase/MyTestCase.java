package testcase;

import junit.framework.TestCase;
import reflection.Config;
import reflection.Util;

public class MyTestCase extends TestCase {
	static Config config;
	
	static {
		try {
			config = Config.readFrom("Dt-config");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void startsWith( String expected, String got) {
		assertEquals( expected, Util.left( got, expected.length() ) );
	}
}
