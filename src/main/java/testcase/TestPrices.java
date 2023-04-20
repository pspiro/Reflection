package testcase;

import static testcase.TestErrors.sendData;

import java.util.HashMap;

import json.MyJsonObject;
import junit.framework.TestCase;
import redis.clients.jedis.Jedis;
import tw.util.S;

public class TestPrices extends TestCase{
	
	static double curPrice = 183;
	static double offset = .7;
	
	public void testOne() throws Exception {
		
		String data = "{ 'msg': 'getprice', 'conid': '8314' }"; 
		MyJsonObject map = sendData( data);
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
		MyJsonObject map = sendData( data);
		HashMap ibm = (HashMap)map.get( "8314");
		double bid = (Double)ibm.get( "bid");
		double ask =  (Double)ibm.get( "ask");
		
		assertTrue( bid >= curPrice - offset && bid <= curPrice + offset);
		assertTrue( ask >= curPrice - offset && ask <= curPrice + offset);
		
		S.out( bid + " " + ask);
	}
	
	/** Note that this corrupts the prices in the redis server. */
	public void testGetPrices() throws Exception {
		String redisHost = "34.125.38.193";
		int redisPort = 3001;
		String conid = "8314";
		String bid = "12.34";
		String ask = "12.36";
		String lst = "12.38";
		
		Jedis jedis = new Jedis(redisHost, redisPort);
		jedis.hset( conid, "bid", bid);
		jedis.hset( conid, "ask", ask);
		jedis.hset( conid, "last", lst);
		
		String data = "{ 'msg': 'getallprices' }"; 
		MyJsonObject map = TestErrors.sendData( data);
		HashMap prices = (HashMap)map.get(conid);
		assertEquals( bid, prices.get("bid").toString() );
		assertEquals( ask, prices.get("ask").toString() );
		
		
		S.out(map);
//		String ret = (String)map.get( "code");
//		String text = (String)map.get( "text");
//		assertEquals( RefCode.EXCHANGE_CLOSED.toString(), ret);
//		assertEquals( text, "Exchange is closed");
	}
}
