package testcase;

import http.MyHttpClient;
import json.MyJsonArray;
import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.Util;
import tw.util.S;

public class TestUnwindOrder extends TestCase {
	static String host = "localhost";

	public void test() throws Exception {
		// get starting position
		double pos1 = getPos(8314);
		S.out( "pos1 = " + pos1);
		
		String data = """
				{ 
				'msg': 'order', 
				'conid': '8314',
				'side': 'buy',
				'quantity': '100',
				'price': '183',
				'fail': true,
				'wallet': '0xb016711702D3302ceF6cEb62419abBeF5c44450e'
				}
				""";

		// place order
		MyJsonObject obj = new MyHttpClient( host, 8383)
				.addHeader("Cookie", Cookie.cookie)
				.post( "/api/order", Util.toJson(data) )
				.readMyJsonObject();

		// get new pos; should be higher
		double pos2 = getPos(8314);
		S.out( "pos2 = " + pos2);
		assertTrue( pos2 > pos1);  // will not work in autoFill mode
		
		// wait for the unwind order to be executed
		S.sleep(4000);
		double pos3 = getPos(8314);
		S.out( "pos3 = " + pos3);
		assertTrue( pos3 == pos1);  // will not work in autoFill mode
	}

	private double getPos(int conid) throws Exception {
		MyJsonArray ar = new MyHttpClient(host, 8383)
				.get("?msg=getpositions")
				.readMyJsonArray();
		MyJsonObject obj = find(ar, 8314);
		return obj != null ? obj.getDouble("conid") : 0;
	}

	private MyJsonObject find(MyJsonArray ar, int conid) {
		for (MyJsonObject obj : ar) {
			if (obj.getInt("conid") == conid) {
				return obj;
			}
		}
		return null;
	}
}
