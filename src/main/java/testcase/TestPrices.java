package testcase;

import static testcase.TestErrors.sendData;

import http.MyHttpClient;
import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.Util;
import tw.util.S;

public class TestPrices extends TestCase{
	
	static double curPrice = 135.75;
	static double offset = .7;
	
	public void testOne() throws Exception {
		
		String data = "{ 'msg': 'getprice', 'conid': '265598' }"; 
		MyJsonObject map = sendData( data);
		S.out( "bid=%s  ask=%s", map.getDouble( "bid"), map.getDouble( "ask") );
		double bid = map.getDouble( "bid");
		assertTrue( bid >= curPrice - offset && bid <= curPrice + offset);
		
		double ask = map.getDouble( "ask");
		assertTrue( ask >= curPrice - offset && ask <= curPrice + offset);
		
		//assertTrue (bid < ask);

//		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
//		assertEquals( text, "Param 'cryptoid' is missing");
	}

	public void testAll() throws Exception {
		String data = "{ 'msg': 'getallprices' }"; 
		
		MyHttpClient cli = new MyHttpClient( "localhost", 8383);
		cli.post( Util.toJson( data) );
		S.out( "getAllPrices");
		String str = cli.readString();
		MyJsonObject mine = MyJsonObject.parse(str);
		mine.display();

//		map.display();
//		Object obj = map.get( "265598");
//		S.out( obj.getClass() + " " + obj);

//		HashMap ibm = (HashMap)map.get( "265598");
//		double bid = (Double)ibm.get( "bid");
//		double ask =  (Double)ibm.get( "ask");
//		
//		assertTrue( bid >= curPrice - offset && bid <= curPrice + offset);
//		assertTrue( ask >= curPrice - offset && ask <= curPrice + offset);
//		
//		S.out( bid + " " + ask);
	}
	
}
