package testcase;

import http.MyHttpClient;
import json.MyJsonObject;
import junit.framework.TestCase;

public class TestGetStocks extends TestCase {
	public void test1() throws Exception {
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/api/reflection-api/get-stock-with-price/8314");
		MyJsonObject obj = cli.readMyJsonObject();
		assertEquals( 200, cli.getResponseCode() );
		assertEquals( "IBM", obj.getString("symbol") );
		assertEquals( "Stock", obj.getString("type") );
		assertEquals( "8314", obj.getString("conid") );
		assertEquals( "open", obj.getString("exchangeStatus") );
	}
}
