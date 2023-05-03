package testcase;

import java.sql.ResultSet;
import java.util.Date;

import http.MyHttpClient;
import json.MyJsonObject;
import junit.framework.TestCase;
import redis.clients.jedis.Jedis;
import reflection.Config;
import reflection.Prices;
import reflection.RefCode;
import reflection.Util;
import tw.util.S;

public class TestOrder extends MyTestCase {
	static double curPrice = 128.15; // Double.valueOf( jedis.hget("8314", "last") );
	static double approved;
	
	static {
		try {
			//seed();
//			MyHttpClient cli = new MyHttpClient("localhost", 8383);
//			cli.get("?msg=seedPrices");

			approved = config.busd().getAllowance(Cookie.wallet, config.rusdAddr() );
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void seed() throws Exception {
		Jedis jedis = config.redisPort() == 0
				? new Jedis( config.redisHost() )  // use full connection string
				: new Jedis( config.redisHost(), config.redisPort() );
		
		jedis.hset( "8314", "bid", "" + curPrice);
		jedis.hset( "8314", "ask", "" + curPrice);
		jedis.hset( "8314", "last", "" + curPrice);
	}
	
	// missing walletId
	public void testMissingWallet() throws Exception {
		MyJsonObject obj = orderData(2, "BUY", 10);
		obj.remove("wallet_public_key");
		MyJsonObject map = postDataToObj(obj);
		String ret = map.getString( "code");
		String text = map.getString("message");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( text, "Param 'wallet_public_key' is missing");
	}
	
	// reject order; price too high; IB won't accept it
	public void testBuyTooHigh() throws Exception {
		MyJsonObject obj = orderData("{ 'msg': 'order', 'conid': '8314', 'action': 'buy', 'quantity': '10', 'tokenPrice': '200', 'cryptoid': 'testmaxamtbuy' }");		
		MyJsonObject map = postDataToObj(obj);
		String code = map.getString( "code");
		String text = map.getString("message");
		S.out( "testOrder4: %s", map);
		assertEquals( RefCode.REJECTED.toString(), code);  // fails if auto-fill is on
		assertEquals( "Reason unknown", text);
	}
	
	// reject order; price too low
	public void testBuyTooLow() throws Exception {
		MyJsonObject obj = orderData( -1, "BUY", 10);
		MyJsonObject map = postDataToObj(obj);
		String code = map.getString( "code");
		String text = map.getString("message");
		S.out( code + " " + text);
		S.out( RefCode.INVALID_PRICE + " " + Prices.TOO_LOW);
		assertEquals( RefCode.INVALID_PRICE.toString(), code);
		assertEquals( Prices.TOO_LOW, text);
	}
	
	// sell order price to high
	public void testSellTooHigh() throws Exception {
		MyJsonObject obj = orderData( 1, "SELL", 10);
		MyJsonObject map = postDataToObj(obj);
		String code = map.getString( "code");
		String text = map.getString("message");
		S.out("sellTooHigh %s %s", code, text);
		assertEquals( RefCode.INVALID_PRICE.toString(), code);
		assertEquals( Prices.TOO_HIGH, text);
	}
	
	// reject order; sell price too low; IB rejects it
	public void testSellPriceTooLow() throws Exception {
		MyJsonObject obj = orderData( -30, "SELL", 100);
		MyJsonObject map = postDataToObj(obj);
		String code = map.getString( "code");
		String text = map.getString("message");
		S.out("sell too low %s %s", code, text);
		assertEquals( RefCode.INVALID_PRICE.toString(), code);  // test fails if autoFill is on
		assertEquals( Prices.TOO_LOW, text);
	}

	// fill order buy order
	public void testFillBuy() throws Exception {
		MyJsonObject obj = TestOrder.orderData( 3, "BUY", 10);
		
		// this won't work because you have to 
		//obj.remove("noFireblocks"); // let the fireblocks go through so we can test the crypto_transaction
		
		MyJsonObject map = postDataToObj(obj);
		String code = map.getString( "code");
		String text = map.getString("message");
		S.out( "fill buy %s %s", code, text);
		assertEquals( RefCode.OK.toString(), code);
		double filled = map.getDouble( "filled");
		assertEquals( 10.0, filled);

		// this part won't work if Fireblocks is turned off
//		ResultSet res = TestOrder.config.sqlConnection().queryNext( "select * from crypto_transactions where id = (select max(id) from crypto_transactions)");
//		assertEquals( Cookie.wallet.toLowerCase(), res.getString("wallet_public_key").toLowerCase() ); 
//		long ts = res.getInt("timestamp");
//		long now = System.currentTimeMillis();
//		S.out( "  now=%s  timestamp=%s", new Date(now).toString(), new Date(ts * 1000).toString() );
//		assertTrue( now / 1000 - ts < 2000); 
	}

	public void testNullCookie() throws Exception {
		MyJsonObject obj = orderData( 3, "BUY", 10);
		obj.remove("cookie");
		
		MyHttpClient cli = postData(obj);
		MyJsonObject map = cli.readMyJsonObject();
		String text = map.getString("message");
		assertEquals( 400, cli.getResponseCode() );
		startsWith( "Null cookie", text);
	}
	
	// fill order sell order
	public void testFillSell() throws Exception {
		MyJsonObject obj = orderData( -3, "sell", 10);
		MyJsonObject map = postDataToObj(obj);
		String code = map.getString( "code");
		String text = map.getString("message");
		S.out( "fillSell %s %s", code, text);
		assertEquals( RefCode.OK.toString(), code);
		double filled = map.getDouble( "filled");
		assertEquals( 10.0, filled);
	}
	
	public void testMaxAmtBuy()  throws Exception {
		MyJsonObject obj = orderData("{ 'msg': 'order', 'conid': '8314', 'action': 'buy', 'quantity': '200', 'tokenPrice': '138', 'cryptoid': 'testmaxamtbuy' }");
		MyJsonObject map = postDataToObj(obj);
		String ret = map.getString( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);
	}

	public void testMaxAmtSell()  throws Exception {
		MyJsonObject obj = orderData("{ 'msg': 'order', 'conid': '8314', 'action': 'sell', 'quantity': '200', 'tokenPrice': '138', 'cryptoid': 'testmaxamtsell' }"); 
		MyJsonObject map = postDataToObj(obj);
		String ret = map.getString( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);
	}

	public void testFracShares()  throws Exception {
		MyJsonObject obj = orderData("{ 'msg': 'order', 'conid': '8314', 'action': 'buy', 'quantity': '1.5', 'tokenPrice': '138' }"); 
		MyJsonObject map = postDataToObj(obj);
		String ret = map.getString( "code");
		String text = map.getString("message");
		S.out( "testFracShares %s %s", ret, text);
		assertEquals( RefCode.OK.toString(), ret);
	}

	public void testSmallOrder()  throws Exception {  // no order should be submitted to exchange
		MyJsonObject obj = orderData("{ 'msg': 'order', 'conid': '8314', 'action': 'buy', 'quantity': '.4', 'tokenPrice': '138' }"); 
		MyJsonObject map = postDataToObj(obj);
		String ret = map.getString( "code");
		assertEquals( RefCode.OK.toString(), ret);
	}

	public void testZeroShares()  throws Exception {
		MyJsonObject obj = orderData("{ 'msg': 'order', 'conid': '8314', 'action': 'buy', 'quantity': '0', 'tokenPrice': '138' }"); 
		MyJsonObject map = postDataToObj(obj);
		String ret = map.getString( "code");
		String text = map.getString("message");
		S.out( "zero shares: " + text);
		assertEquals( "Quantity must be positive", text);
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
	}
	
	static MyJsonObject orderData(String json) throws Exception {
		MyJsonObject obj = MyJsonObject.parse( Util.toJson(json) );
		addCookie(obj);
		return obj;
	}
	
	static MyJsonObject orderData(double offset, String side, double qty) throws Exception {
		String json = String.format( "{ 'conid': '8314', 'action': '%s', 'quantity': %s, 'tokenPrice': '%s', 'tds': 1.11 }",
				side, qty, Double.valueOf( curPrice + offset) );
		MyJsonObject obj = MyJsonObject.parse( Util.toJson(json) );
		addCookie(obj);
		return obj;
	}
	
	static MyJsonObject addCookie(MyJsonObject obj) throws Exception {
		obj.put("cookie", Cookie.cookie);
		obj.put("noFireblocks", true);
		obj.put("currency", "busd");
		obj.put("wallet_public_key", Cookie.wallet);
		
		double price = obj.getDouble("tokenPrice");
		double qty = obj.getDouble("quantity");
		double comm = obj.getDouble("commission");
		double total = obj.getString("action").equals("buy")
				? price * qty + comm : price * qty - comm;
		obj.put("price", total);
		return obj;
	}

	static MyJsonObject postDataToObj( MyJsonObject obj) throws Exception {
		return postData(obj).readMyJsonObject();
	}
	
	static MyHttpClient postData( MyJsonObject obj) throws Exception {
		return cli().post( "/api/reflection-api/order", obj.toString() ); 
	}
}
