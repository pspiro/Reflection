package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import web3.NodeInstance;

public class TestPositionsNew extends MyTestCase {
	static String host = "localhost"; // prod = "34.125.38.193";
	
	public void testTokenPos() throws Exception {
		Cookie.setWalletAddr( NodeInstance.prod);
		cli().post("/api/positions-new/" + NodeInstance.prod, Cookie.getJson() );
		assert200();
		JsonArray ar = cli.readJsonArray();
		assertTrue( ar.size() > 0);
		
		JsonObject item = ar.getJsonObj(0);
		item.display();

		assertTrue( item.getString("symbol").length() > 0 );
		assertTrue( item.getDouble("quantity") > 0);
		assertTrue( item.getDouble("price") > 0);
		assertTrue( item.getInt("conId") > 0);
		assertTrue( item.getDouble("pnl") != 0);
	}
	
}
