package testcase;

import org.json.simple.JsonObject;

import reflection.Prices;
import reflection.RefCode;
import tw.util.S;

// these test fail because the initial order is accepted, then later it is rejected
public class TestOrderNoAutoFill extends MyTestCase {
	static double curPrice;
	static boolean m_noFireblocks = true;
//	static double approved;
	

	// reject order; price too high; IB won't accept it
	public void testBuyTooHigh() throws Exception {
		JsonObject obj = TestOrder.createOrderWithOffset( "BUY", 1, 30);
		JsonObject map = postOrderToObj(obj);
		String code = map.getString( "code");
		String text = map.getString("message");
		S.out( "testOrder4: %s", map);
		assertEquals( RefCode.REJECTED.toString(), code);  // fails if auto-fill is on
		assertEquals( "Reason unknown", text);
	}
	
	// reject order; sell price too low; IB rejects it
	public void testSellTooLow() throws Exception {
		JsonObject obj = TestOrder.createOrderWithOffset( "SELL", 1, -30);
		JsonObject map = postOrderToObj(obj);
		String code = map.getString( "code");
		String text = map.getString("message");
		S.out("sell too low %s %s", code, text);
		assertEquals( RefCode.INVALID_PRICE.toString(), code);  // test fails if autoFill is on
		assertEquals( Prices.TOO_LOW, text);
	}
}
