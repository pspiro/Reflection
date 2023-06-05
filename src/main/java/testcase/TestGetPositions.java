package testcase;

import java.util.Timer;
import java.util.TimerTask;

import http.MyHttpClient;
import json.MyJsonArray;
import json.MyJsonObject;
import junit.framework.TestCase;
import tw.util.S;

public class TestGetPositions extends MyTestCase {
	static String wallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	static String host = "localhost"; // prod = "34.125.38.193";
	
	public static void main(String[] args) {
		Timer timmer = new Timer();
		timmer.schedule(new TimerTask() {
			public void run() {
				S.out( "hello");
			}
		}, 0);
		timmer.schedule(new TimerTask() {
			public void run() {
				S.out( "hello");
			}
		}, 0);
		timmer.schedule(new TimerTask() {
			public void run() {
				S.out( "hello");
			}
		}, 0, 3000);
	}
	
	public void testStockPos() throws Exception {
		MyHttpClient cli = cli();
		cli.get("?msg=getpositions");
		assertEquals(200, cli.getResponseCode());

		MyJsonArray ar = cli.readMyJsonArray();
		assertTrue( ar.size() > 0);

		MyJsonObject item = ar.getJsonObj(0);
		item.display();
		assertTrue( item.getInt("conid") > 0);
		assertTrue( item.getDouble("position") > 0);
	}
	
	public void testTokenPos() throws Exception {
		MyHttpClient cli = cli();
		cli.get("/api/positions/" + wallet);
		assertEquals(200, cli.getResponseCode());
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
