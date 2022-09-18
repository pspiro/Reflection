package test;

import java.util.HashMap;

import junit.framework.TestCase;
import reflection.RefCode;

// this should be before open, after close, and on days that the exchange is closed
public class TestOutsideHours extends TestCase {
	public void testCheckOrder() throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'side': 'buy', 'quantity': '100', 'price': '83', 'wallet': '0x747474',  }"; 
		HashMap<String, Object> map = TestErrors.sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.EXCHANGE_CLOSED.toString(), ret);
		assertEquals( text, "Exchange is closed");
	}

	public void testOrder() throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '100', 'price': '83', 'wallet': '0x747474', 'cryptoid': 'abcd' }"; 
		HashMap<String, Object> map = TestErrors.sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.EXCHANGE_CLOSED.toString(), ret);
		assertEquals( text, "Exchange is closed");
	}

	public void testGetPrices() throws Exception {
		String data = "{ 'msg': 'getprices' }"; 
		HashMap<String, Object> map = TestErrors.sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.EXCHANGE_CLOSED.toString(), ret);
		assertEquals( text, "Exchange is closed");
	}
}
