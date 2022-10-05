package test;

import static test.TestErrors.sendData;

import java.util.HashMap;

import junit.framework.TestCase;
import reflection.RefCode;
import tw.util.S;

public class TestOne extends TestCase {
	public void testFracShares()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '1.5', 'price': '122', 'wallet': '8383', 'cryptoid': 'testfracshares' }"; 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		S.out( map.get( "text") );
		assertEquals( RefCode.OK.toString(), ret);
	}

} 	
