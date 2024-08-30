package testcase;


import org.json.simple.JsonArray;

import http.MyHttpClient;
import junit.framework.TestCase;

public class TestFaqs extends MyTestCase {
	public void testFaqs() throws Exception {
		JsonArray obj = cli().get( "/api/faqs").readJsonArray();
		obj.display();
		assertTrue(obj.size() > 3);
	}
}
