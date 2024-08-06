package testcase;


import org.json.simple.JsonArray;

import http.MyHttpClient;

public class TestFaqs extends MyTestCase {
	public void testFaqs() throws Exception {
		MyHttpClient cli = new MyHttpClient();
		JsonArray obj = cli.get( "/api/faqs").readJsonArray();
		obj.display();
		assertTrue(obj.size() > 3);
		
	}
}
