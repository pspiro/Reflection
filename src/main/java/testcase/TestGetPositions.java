package testcase;

import http.MyHttpClient;
import json.MyJsonArray;
import json.MyJsonObject;
import junit.framework.TestCase;

public class TestGetPositions extends TestCase {
	static String wallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	static String host = "localhost"; // prod = "34.125.38.193";
	
	static {
		try {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void testStockPos() throws Exception {
		MyHttpClient cli = new MyHttpClient(host, 8383);
		cli.get("?msg=getpositions");
		MyJsonArray ar = cli.readMyJsonArray();
		assertTrue( ar.size() > 0);

		MyJsonObject item = ar.getJsonObj(0);
		item.display();
		assertTrue( item.getInt("conid") > 0);
		assertTrue( item.getDouble("position") > 0);
	}
	
	public void testTokenPos() throws Exception {
		MyHttpClient cli = new MyHttpClient(host, 8383);
		cli.get("/api/reflection-api/positions/" + wallet);
		MyJsonArray ar = cli.readMyJsonArray();
		assertTrue( ar.size() > 0);
		
		MyJsonObject item = ar.getJsonObj(0);
		item.display();
		assertTrue( item.getString("symbol").length() > 0 );
		assertTrue( item.getDouble("quantity") > 0);
		assertTrue( item.getDouble("price") > 0);
		assertTrue( item.getInt("conId") > 0);
	}
	
}
