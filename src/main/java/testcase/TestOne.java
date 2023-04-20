package testcase;

import static testcase.TestOrder.orderData;
import static testcase.TestOrder.post;

import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.RefCode;
import tw.util.S;

public class TestOne extends TestCase {
	public static void main(String[] args) {
		//Config.readFrom("Desktop-config").newBusd().mint(1, wallet, 800).waitForHash();
	}

	// test what-if success as well
	public void testFillBuy() throws Exception {
		String data = orderData( 3, "BUY", Cookie.wallet);
		MyJsonObject map = post( data);
		String code = map.getString( "code");
		String text = map.getString( "text");
		S.out( "testFillBuy: " + text);
		assertEquals( RefCode.OK.toString(), code);
		double filled = map.getDouble( "filled");
		assertEquals( 100.0, filled);
	}
}
