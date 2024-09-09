package testcase;

import java.util.Timer;
import java.util.TimerTask;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import http.MyHttpClient;
import tw.util.S;

public class TestGetPositions extends MyTestCase {
	static String wallet = "0x2703161D6DD37301CEd98ff717795E14427a462B";
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
		cli.get("/api/?msg=getpositions");
		assert200_();

		JsonArray ar = cli.readJsonArray();
		assertTrue( ar.size() > 0);

		JsonObject item = ar.getJsonObj(0);
		item.display();
		assertTrue( item.getInt("conid") > 0);
		assertTrue( item.getDouble("position") > 0);
	}
	
	public void testTokenPos() throws Exception {
		MyHttpClient cli = cli();
		cli.get("/api/positions-new/" + wallet);
		assert200_();
		JsonArray ar = cli.readJsonArray();
		assertTrue( ar.size() > 0);
		
		JsonObject item = ar.getJsonObj(0);
		item.display();

		assertTrue( item.getString("symbol").length() > 0 );
		assertTrue( item.getDouble("quantity") > 0);
		assertTrue( item.getDouble("price") > 0);
		assertTrue( item.getInt("conId") > 0);
	}
	
}
