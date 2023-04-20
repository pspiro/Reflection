package testcase;

import static testcase.TestBackendOrder.orderData;
import static testcase.TestBackendOrder.sendData;

import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.Prices;
import reflection.RefCode;
import tw.util.S;

public class TestOne extends TestCase {
	public static void main(String[] args) {
		//Config.readFrom("Desktop-config").newBusd().mint(1, wallet, 800).waitForHash();
	}

	public void testSellTooHigh() throws Exception {
		String data = orderData( 1, "SELL", 100);
		MyJsonObject map = sendData( data);
		String code = map.getString( "code");
		String text = map.getString( "text");
		S.out("sellTooHigh %s %s", code, text);
		assertEquals( RefCode.INVALID_PRICE.toString(), code);
		assertEquals( Prices.TOO_HIGH, text);
	}
}
