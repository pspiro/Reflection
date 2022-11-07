package test;

import static test.TestErrors.sendData;

import java.text.SimpleDateFormat;
import java.util.HashMap;

import junit.framework.TestCase;
import reflection.Prices;
import reflection.RefCode;
import tw.util.S;

public class TestOne extends TestCase {
	public void testFracSize()  throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'side': 'buy', 'quantity': '1.5', 'price': '138' }"; 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		assertEquals( RefCode.OK.toString(), ret);
	}
}
