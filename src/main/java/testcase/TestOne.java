package testcase;

import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.RefCode;

public class TestOne extends TestCase {
	public void testWhatIfSuccess() throws Exception {
		double price = TestWhatIf.curPrice + 2;
		
		String data = String.format( "{ 'msg': 'checkorder', 'conid': '8314', 'action': 'buy', 'quantity': '100', 'price': '%s' }", price); 
		MyJsonObject map = TestWhatIf.post( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.OK.toString(), ret);
	}
}
