package test;

import static test.TestErrors.sendData;

import java.text.SimpleDateFormat;
import java.util.HashMap;

import junit.framework.TestCase;
import reflection.Prices;
import reflection.RefCode;
import tw.util.S;

public class TestOne extends TestCase {
	public void testOrder35() throws Exception {
		String data = TestOrder.orderData( 1, "SELL", "pricetoohigh");
		HashMap<String, Object> map = sendData( data);
		String code = (String)map.get( "code");
		String text = (String)map.get( "text");
		S.out( code + " " + text);
		assertEquals( RefCode.INVALID_PRICE.toString(), code);
		assertEquals( Prices.TOO_HIGH, text);
	}
}
