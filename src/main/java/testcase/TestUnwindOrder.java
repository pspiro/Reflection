package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import tw.util.S;

/** This test should work if autoFill is turned off AND IB actually 
 *  fills the order in the paper system */
public class TestUnwindOrder extends MyTestCase {
	static String host = "localhost";

	public void test() throws Exception {
		// get starting position
		double pos1 = getPos(265598);
		S.out( "pos1 = " + pos1);
		
		// place order set to fail
		JsonObject order = TestOrder.createOrder2("BUY", 100, TestOrder.curPrice + 2);
		order.put("fail", true);
		
		postOrderToObj(order);

		// get new pos; should be higher
		double pos2 = getPos(265598);
		S.out( "pos2 = " + pos2);
		assertTrue( pos2 > pos1);  // will not work in autoFill mode
		
		// wait for the unwind order to be executed
		S.sleep(4000);
		double pos3 = getPos(265598);
		S.out( "pos3 = " + pos3);
		assertTrue( pos3 == pos1);  // will not work in autoFill mode
	}

	/** Returns the stock position in IB account */
	private double getPos(int conid) throws Exception {
		JsonArray ar = cli()
				.get("/api/?msg=getpositions")
				.readJsonArray();
		JsonObject obj = find(ar, 265598);
		return obj != null ? obj.getDouble("conid") : 0;
	}

	private JsonObject find(JsonArray ar, int conid) {
		for (JsonObject obj : ar) {
			if (obj.getInt("conid") == conid) {
				return obj;
			}
		}
		return null;
	}
}
