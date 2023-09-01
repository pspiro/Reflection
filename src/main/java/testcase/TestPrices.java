package testcase;

import static testcase.TestErrors.sendData;

import org.json.simple.JsonObject;

import tw.util.S;

public class TestPrices extends MyTestCase{
	
	static double curPrice = TestOrder.curPrice;
	static double offset = .7;
	
	public void testOne() throws Exception {
		
		String data = "{ 'msg': 'getprice', 'conid': '265598' }"; 
		JsonObject map = sendData( data);
		S.out( "bid=%s  ask=%s", map.getDouble( "bid"), map.getDouble( "ask") );
		double bid = map.getDouble( "bid");
		assertTrue( bid >= curPrice - offset && bid <= curPrice + offset);
		
		double ask = map.getDouble( "ask");
		assertTrue( ask >= curPrice - offset && ask <= curPrice + offset);
		
		assertTrue (bid < ask);

//		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
//		assertEquals( text, "Param 'cryptoid' is missing");
	}

	public void testShowPrices() throws Exception {
		cli().get( "/?msg=getallprices").readJsonObject().display();
	}
	
}
