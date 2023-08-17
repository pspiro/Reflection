package testcase;


import org.json.simple.JsonArray;

import http.MyHttpClient;
import junit.framework.TestCase;

public class TestFaqs extends TestCase {
	public void testFaqs() throws Exception {
		MyHttpClient cli = new MyHttpClient( "localhost", 8383);
		JsonArray obj = cli.get( "/api/faqs").readJsonArray();
		obj.display();
		assertTrue(obj.size() > 3);
		
	}
}
