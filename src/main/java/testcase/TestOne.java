package testcase;

import json.MyJsonObject;
import reflection.RefCode;
import tw.util.S;

public class TestOne extends MyTestCase {
	public void testFillBuy() throws Exception {
		MyJsonObject obj = TestOrder.orderData( -1, "SELL", 2);
		obj.display();
		
		// this won't work because you have to 
		//obj.remove("noFireblocks"); // let the fireblocks go through so we can test the crypto_transaction
		
		MyJsonObject map = TestOrder.postDataToObj(obj);
		String code = map.getString( "code");
		String text = map.getString("message");
		S.out( "fill buy %s %s", code, text);
		assertEquals( RefCode.OK.toString(), code);
		double filled = map.getDouble( "filled");
		assertEquals( 2, filled);
	}
}
