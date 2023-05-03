package testcase;

import http.MyHttpClient;
import json.MyJsonObject;
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
	
	static MyHttpClient cli() throws Exception {
		return new MyHttpClient("localhost", 8383);
	}

	public static void startsWith( String expected, String got) {
		assertEquals( expected, Util.left( got, expected.length() ) );
	}
}
