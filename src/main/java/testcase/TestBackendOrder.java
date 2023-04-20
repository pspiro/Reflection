package testcase;

import org.json.simple.JSONObject;

import com.moonstoneid.siwe.SiweMessage;

import http.MyHttpClient;
import json.MyJsonObject;
import junit.framework.TestCase;
import redis.clients.jedis.Jedis;
import reflection.Config;
import reflection.Main;
import reflection.Prices;
import reflection.RefCode;
import reflection.SiweUtil;
import reflection.Util;
import tw.util.S;

public class TestBackendOrder extends TestCase {
	static double curPrice = 135.75; // Double.valueOf( jedis.hget("8314", "last") );
	
	static {
		try {
			seed();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void seed() throws Exception {
		Config config = Config.readFrom("Desktop-config");

		Jedis jedis = config.redisPort() == 0
				? new Jedis( config.redisHost() )  // use full connection string
				: new Jedis( config.redisHost(), config.redisPort() );
		
		jedis.hset( "8314", "bid", "" + curPrice);
		jedis.hset( "8314", "ask", "" + curPrice);
		jedis.hset( "8314", "last", "" + curPrice);
	}
	
	// missing walletId
	public void testMissingWallet() throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'action': 'buy', 'quantity': '100', 'tokenPrice': '83' }";
		MyJsonObject map = sendData( data);
		
//		MyHttpClient cli = new MyHttpClient( "localhost", 8383);
//		//cli.addHeader("Cookie", Cookie.cookie)
//		String obj = Cookie.addCookie( Util.toJson(data) );
//		cli.post( "/api/reflection-api/order",  );
//		
		
		
		String ret = map.getString( "code");
		String text = map.getString( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( text, "Param 'wallet_public_key' is missing");
	}
	
	// reject order; price too high; IB won't accept it
	public void testBuyTooHigh() throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'action': 'buy', 'quantity': '10', 'tokenPrice': '200', 'cryptoid': 'testmaxamtbuy' }";		
		MyJsonObject map = sendData( data);
		String code = map.getString( "code");
		String text = map.getString( "text");
		S.out( "testOrder4: %s", map);
		assertEquals( RefCode.REJECTED.toString(), code);  // fails if auto-fill is on
		assertEquals( "Reason unknown", text);
	}
	
	// reject order; price too low
	public void testBuyTooLow() throws Exception {
		String data = orderData( -1, "BUY", 10);
		MyJsonObject map = sendData( data);
		String code = map.getString( "code");
		String text = map.getString( "text");
		S.out( code + " " + text);
		S.out( RefCode.INVALID_PRICE + " " + Prices.TOO_LOW);
		assertEquals( RefCode.INVALID_PRICE.toString(), code);
		assertEquals( Prices.TOO_LOW, text);
	}
	
	// sell order price to high
	public void testSellTooHigh() throws Exception {
		String data = orderData( 1, "SELL", 10);
		MyJsonObject map = sendData( data);
		String code = map.getString( "code");
		String text = map.getString( "text");
		S.out("sellTooHigh %s %s", code, text);
		assertEquals( RefCode.INVALID_PRICE.toString(), code);
		assertEquals( Prices.TOO_HIGH, text);
	}
	
	// reject order; sell price too low; IB rejects it
	public void testSellPriceTooLow() throws Exception {
		String data = orderData( -30, "SELL", 100);
		MyJsonObject map = sendData( data);
		String code = map.getString( "code");
		String text = map.getString( "text");
		S.out("sell too low %s %s", code, text);
		assertEquals( RefCode.INVALID_PRICE.toString(), code);  // test fails if autoFill is on
		assertEquals( Prices.TOO_LOW, text);
	}

	// fill order buy order
	public void testFillBuy() throws Exception {
		String data = orderData( 3, "BUY", 10);
		MyJsonObject map = sendData( data);
		String code = map.getString( "code");
		String text = map.getString( "text");
		S.out( "fill buy %s %s", code, text);
		assertEquals( RefCode.OK.toString(), code);
		double filled = map.getDouble( "filled");
		assertEquals( 10.0, filled);
	}

	public void testNullCookie() throws Exception {
		String data = orderData( 3, "BUY", 100);

		MyHttpClient cli = new MyHttpClient( "localhost", 8383);
		cli.post( "/api/reflection-api/order", Util.toJson(data) );
		MyJsonObject map = cli.readMyJsonObject();            
		String text = map.getString( "text");
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( "Null cookie", text);
	}
	
	// fill order sell order
	public void testFillSell() throws Exception {
		String data = orderData( -3, "sell", 100);
		MyJsonObject map = sendData( data);
		String code = map.getString( "code");
		String text = map.getString( "text");
		S.out( "fillSell %s %s", code, text);
		assertEquals( RefCode.OK.toString(), code);
		double filled = map.getDouble( "filled");
		assertEquals( 100.0, filled);
	}
	
	public void testMaxAmtBuy()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'action': 'buy', 'quantity': '200', 'tokenPrice': '138', 'cryptoid': 'testmaxamtbuy' }";
		MyJsonObject map = sendData( data);
		String ret = map.getString( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);
	}

	public void testMaxAmtSell()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'action': 'sell', 'quantity': '200', 'tokenPrice': '138', 'cryptoid': 'testmaxamtsell' }"; 
		MyJsonObject map = sendData( data);
		String ret = map.getString( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);
	}

	public void testFracShares()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'action': 'buy', 'quantity': '1.5', 'tokenPrice': '138' }"; 
		MyJsonObject map = sendData( data);
		String ret = map.getString( "code");
		String text = map.getString( "text");
		S.out( "testFracShares %s %s", ret, text);
		assertEquals( RefCode.OK.toString(), ret);
	}

	public void testSmallOrder()  throws Exception {  // no order should be submitted to exchange
		String data = "{ 'msg': 'order', 'conid': '8314', 'action': 'buy', 'quantity': '.4', 'tokenPrice': '138' }"; 
		MyJsonObject map = sendData( data);
		String ret = map.getString( "code");
		assertEquals( RefCode.OK.toString(), ret);
	}

	public void testZeroShares()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'action': 'buy', 'quantity': '0', 'tokenPrice': '138' }"; 
		MyJsonObject map = sendData( data);
		String ret = map.getString( "code");
		String text = map.getString( "text");
		assertEquals( "Quantity must be positive", text);
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
	}
	
	static String orderData(double offset, String side, double qty) {
		return String.format( "{ 'conid': '8314', 'action': '%s', 'quantity': %s, 'tokenPrice': '%s', 'tds': 1.11 }",
				side, qty, Double.valueOf( curPrice + offset) );
	}
	
	static MyJsonObject sendData( String data) throws Exception {
		MyHttpClient cli = new MyHttpClient( "localhost", 8383);
		//cli.addHeader("Cookie", Cookie.cookie)
		cli.post( "/api/reflection-api/order", Cookie.addCookie( Util.toJson(data) ) );
		
		return cli.readMyJsonObject();
	}
}
