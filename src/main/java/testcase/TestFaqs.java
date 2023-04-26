package testcase;


import http.MyHttpClient;
import json.MyJsonArray;
import json.MyJsonObject;
import junit.framework.TestCase;

public class TestFaqs extends TestCase {
	public void testFaqs() throws Exception {
		MyHttpClient cli = new MyHttpClient( "localhost", 8383);
		MyJsonArray obj = cli.get( "/api/faqs").readMyJsonArray();
		obj.display();
		assertTrue(obj.size() > 3);
		
	}
}
