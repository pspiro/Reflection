package testcase;

import json.MyJsonObject;
import reflection.RefCode;
import tw.util.S;

public class TestOne extends MyTestCase {
	public void testFracShares()  throws Exception {
		MyJsonObject obj = TestOrder.orderData("{ 'msg': 'order', 'conid': '8314', 'action': 'buy', 'quantity': '1.5', 'tokenPrice': '138' }"); 
		MyJsonObject map = TestOrder.postDataToObj(obj);
		String ret = map.getString( "code");
		String text = map.getString("message");
		S.out( "testFracShares %s %s", ret, text);
		assertEquals( RefCode.OK.toString(), ret);
	}
}
