package test;

import static test.TestErrors.sendData;

import java.util.HashMap;

import junit.framework.TestCase;
import reflection.RefCode;

public class SimpleTest extends TestCase {
	public static void main(String[] args) throws Exception {
		double price = TestOrder.curPrice + .3;
		
		String data = String.format( "{ 'msg': 'checkorder', 'conid': '8314', 'side': 'buy', 'quantity': '100', 'price': '%s' }", price); 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.OK.toString(), ret);
		assertEquals( null, text);
	}

}
