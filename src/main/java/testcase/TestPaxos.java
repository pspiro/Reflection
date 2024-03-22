package testcase;

import org.json.simple.JsonObject;

import common.Util;
import http.MyHttpClient;
import tw.util.S;

public class TestPaxos extends MyTestCase {
	static double curPrice;
	static int conid = 479624278;
	
	static {
		try {
			// get current price
			JsonObject json = new MyHttpClient("localhost", 8383) 
					.get( "/api/get-price/" + conid)
					.readJsonObject();
			curPrice = (json.getDouble("bid") + json.getDouble("ask") ) / 2;
			S.out( "TestOrder: Current BTC price is %s", curPrice);
			Util.require( curPrice > 0, "Zero price");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	// fill order buy order
	public void testFillBuy() throws Exception {
		JsonObject json = Util.toJson(
			"conid", conid,
			"action", "buy",
			"quantity", .1,
			"tokenPrice", curPrice * 1.1
		);
		
		JsonObject obj = TestOrder.createOrder3(json.toString(), false);
		S.out( "Placing bitcoin order " + obj);
		
		JsonObject map = postOrderToObj(obj);
		assert200();
		
		JsonObject ret = getLiveMessage(map.getString("id"));
		assertEquals( "message", ret.getString("type") );
		startsWith( "Bought 10", ret.getString("text") );
	}
	
}
