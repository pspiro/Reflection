package test;

import static test.TestErrors.sendData;

import java.util.HashMap;

import junit.framework.TestCase;
import tw.util.S;

public class TestPrices extends TestCase{
	
	static double curPrice = TestOrder.curPrice;
	static double offset = .7;
	
	public void testOne() throws Exception {
		
		String data = "{ 'msg': 'getprice', 'conid': '8314' }"; 
		HashMap<String, Object> map = sendData( data);
		S.out( "bid=%s  ask=%s", map.get( "bid"), map.get( "ask") );
		double bid = (Double)map.get( "bid");
		assertTrue( bid >= curPrice - offset && bid <= curPrice + offset);
		
		double ask = (Double)map.get( "ask");
		assertTrue( ask >= curPrice - offset && ask <= curPrice + offset);
		
		assertTrue (bid < ask);

//		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
//		assertEquals( text, "Param 'cryptoid' is missing");
	}

	public void testAll() throws Exception {
		String data = "{ 'msg': 'getallprices' }"; 
		HashMap<String, Object> map = sendData( data);
		HashMap ibm = (HashMap)map.get( "8314");
		double bid = (Double)ibm.get( "bid");
		double ask =  (Double)ibm.get( "ask");
		
		assertTrue( bid >= curPrice - offset && bid <= curPrice + offset);
		assertTrue( ask >= curPrice - offset && ask <= curPrice + offset);
		
		S.out( bid + " " + ask);
	}
}
