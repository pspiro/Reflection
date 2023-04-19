package testcase;

import static testcase.TestOrder.orderData;
import static testcase.TestOrder.post;

import fireblocks.Accounts;
import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.RefCode;

public class TestOne extends TestCase {
	public void testFillBuy() throws Exception {
		String data = orderData( 3);
		MyJsonObject map = post( data);
		String code = map.getString( "code");
		String text = map.getString( "text");
		assertEquals( RefCode.OK.toString(), code);
		double filled = map.getDouble( "filled");
		assertEquals( 100.0, filled);
	}
}
