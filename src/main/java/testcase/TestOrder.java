package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import fireblocks.Rusd;
import http.MyHttpClient;
import reflection.Main;
import reflection.Prices;
import reflection.RefCode;
import tw.util.S;

public class TestOrder extends MyTestCase {
	static double curPrice;
	
//	static double approved;
	
	static {
		try {
			JsonObject json = new MyHttpClient("localhost", 8383) 
					.get( "/api/get-price/265598")
					.readJsonObject();
			curPrice = (json.getDouble("bid") + json.getDouble("ask") ) / 2;
			S.out( "TestOrder: Current AMZN price is %s", curPrice);
			Util.require( curPrice > 0, "Zero price");
		//	approved = config.busd().getAllowance(Cookie.wallet, config.rusdAddr() );
			
//			readStocks();
//			m_config.rusd().mintRusd( Cookie.wallet, 5000, stocks.getAnyStockToken() )
//				.waitForCompleted();
//			waitForRusdBalance(Cookie.wallet, 5000, false);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	


	// test missing and invalid user rec
	public void testMissingUserRec() throws Exception {
		// test missing user rec
		m_config.sqlCommand( conn -> conn.delete("delete from users where wallet_public_key = '%s'", Cookie.wallet.toLowerCase()) );
		JsonObject obj = createOrder("BUY", 10, 2);
		postOrderToObj(obj);
		assertEquals( RefCode.INVALID_USER_PROFILE, cli.getRefCode() );

		// test missing pan
		m_config.sqlCommand( conn -> conn.insertJson("users", TestProfile.createProfileNC() ) );
		m_config.sqlCommand( conn -> conn.delete("update users set pan_number = '8383' where wallet_public_key = '%s'", Cookie.wallet.toLowerCase()) );
		obj = createOrder("BUY", 10, 2);
		postOrderToObj(obj);

		// test success
		m_config.sqlCommand( conn -> conn.execute( String.format("update users set pan_number = 'AAAAA8888A' where wallet_public_key = '%s'", Cookie.wallet.toLowerCase()) ) );
		obj = createOrder("BUY", 10, 2);
		postOrderToObj(obj);
		assert200();
	}
	
	// missing walletId
//	public void testMissingUserAttrib() throws Exception {
//		JsonObject obj = createOrder("BUY", 10, 2);
//		obj.remove("wallet_public_key");
//		JsonObject map = postOrderToObj(obj);
//		String ret = map.getString( "code");
//		String text = map.getString("message");
//		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
//		assertEquals( text, "Param 'wallet_public_key' is missing");
//	}
	
	// missing walletId
	public void testMissingWallet() throws Exception {
		JsonObject obj = createOrder("BUY", 10, 2);
		obj.remove("wallet_public_key");
		JsonObject map = postOrderToObj(obj);
		String ret = map.getString( "code");
		String text = map.getString("message");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( text, "Param 'wallet_public_key' is missing");
	}
	
	// reject order; price too low
	public void testBuyTooLow() throws Exception {
		JsonObject obj = createOrder( "BUY", 10, -1);
		JsonObject map = postOrderToObj(obj);
		String code = map.getString( "code");
		String text = map.getString("message");
		S.out( code + " " + text);
		S.out( RefCode.INVALID_PRICE + " " + Prices.TOO_LOW);
		assertEquals( RefCode.INVALID_PRICE.toString(), code);
		assertEquals( Prices.TOO_LOW, text);
	}
	
	// sell order price to high
	public void testSellTooHigh() throws Exception {
		JsonObject obj = createOrder( "SELL", 10, 1);
		JsonObject map = postOrderToObj(obj);
		String code = map.getString( "code");
		String text = map.getString("message");
		S.out("sellTooHigh %s %s", code, text);
		assertEquals( RefCode.INVALID_PRICE.toString(), code);
		assertEquals( Prices.TOO_HIGH, text);
	}
	
	public void testTransTableEntry() throws Exception {
		JsonObject obj = createOrder( "BUY", 10, -1);
		JsonObject map = postOrderToObj(obj);
		assertEquals(400, cli.getResponseCode() );
		S.sleep(Main.DB_PAUSE + 200); // wait for db insertion
		S.out(map);
		JsonArray ar = m_config.sqlQuery( sql -> sql.queryToJson( "select status from transactions order by created_at desc limit 1") );
		assertEquals("DENIED", ar.get(0).getString("status"));
	}

	/** Test that order fails if the siwe session has timed out */
	public void testSessionTimeout() throws Exception {
		Cookie.init = true;  // force signin
		
		modifySetting( "sessionTimeout", "500", () -> {
			S.out( "sleeping");
			S.sleep(1000);
			
			postOrderToObj( TestOrder.createOrder( "BUY", 10, 3) );
			assertEquals( RefCode.VALIDATION_FAILED, cli.getRefCode() ); 
		});
	}

	// fill order buy order
	public void testFillBuy() throws Exception {
		postOrderToObj( TestOrder.createOrder( "BUY", 10, 3) );  // try again w/ autofill off
		assert200();
		assertEquals( RefCode.OK, cli.getRefCode() );
		S.out( "received %s %s", cli.getRefCode(), cli.getMessage() );
		
		S.sleep(50);  // give it time to complete processing after ok is sent back 
		JsonObject ret = getLiveMessage2( cli.getUId() );
		assertEquals( "message", ret.getString("type") );
		assertEquals( "Filled", ret.getString( "status") );
		startsWith( "Bought 10", ret.getString("text") );
	}

	/** When the IB order is filled, we get a message back saying
	 *  the order is being processed on the blockchain */
	public void testToast() throws Exception {
		JsonObject ord = TestOrder.createOrder( "BUY", 1, 3);
		ord.remove( "noFireblocks");
		postOrderToObj( ord );  // try again w/ autofill off
		assert200();
		
		S.sleep(50);
		JsonObject ret = getLiveMessage2( cli.getUId() );
		assertEquals( "message", ret.getString("type") );
		startsWith( "Your order was filled", ret.getString("text") );
	}

	public void testNullCookie() throws Exception {
		JsonObject obj = createOrder( "BUY", 10, 3);
		obj.remove("cookie");
		
		MyHttpClient cli = postOrder(obj);
		JsonObject map = cli.readJsonObject();
		String text = map.getString("message");
		assertEquals( 400, cli.getResponseCode() );
		startsWith( "Null cookie", text);
	}
	
	// fill order sell order
	public void testFillSell() throws Exception {
		JsonObject obj = createOrder( "sell", 1, -3);
		JsonObject map = postOrderToObj(obj);
		String code = map.getString( "code");
		String text = map.getString("message");
		S.out( "fillSell %s %s", code, text);
		assertEquals(RefCode.OK, cli.getRefCode() );
		assert200();

		S.sleep(50);  // give it time to complete processing after ok is sent back 
		JsonObject ret = getLiveMessage2(map.getString("id"));
		assertEquals( "message", ret.getString("type") );
		assertEquals( "Filled", ret.getString( "status") );
		startsWith( "Sold 1", ret.getString("text") );
	}

	// user must have passed KYC for this to pass
	public void testMinOrderSize() throws Exception {
		// fail buy
		JsonObject map = placeOrder( "buy", m_config.minOrderSize() - .1, curPrice * 1.1);
		String ret = map.getString( "code");
		assertEquals( RefCode.ORDER_TOO_SMALL.toString(), ret);

		// fail sell
		map = placeOrder( "sell", m_config.minOrderSize() - .1, curPrice * .9);
		ret = map.getString( "code");
		assertEquals( RefCode.ORDER_TOO_SMALL.toString(), ret);

		// succeed buy
		map = placeOrder( "buy", m_config.minOrderSize() + .1, curPrice * 1.1);
		assertEquals( RefCode.OK, cli.getRefCode() );
		assert200();

		// succeed sell
		map = placeOrder( "sell", m_config.minOrderSize() + .1, curPrice * .9);
		assert200();
	}
	
	// user must have passed KYC for this to pass
	public void testMaxAmt()  throws Exception {
		// fail buy
		JsonObject map = placeOrder( "buy", m_config.maxOrderSize() + 1, curPrice * 1.1);
		String ret = map.getString( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);

		// fail sell
		map = placeOrder( "sell", m_config.maxOrderSize() + 1, curPrice * .9);
		ret = map.getString( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);

		// succeed buy
		map = placeOrder( "buy", m_config.maxOrderSize() - 1, curPrice * 1.1);
		assert200();

		// succeed sell
		map = placeOrder( "sell", m_config.maxOrderSize() - 1, curPrice * .9);
		assert200();
	}
	
	private JsonObject placeOrder( String side, double amt, double price) throws Exception {
		S.out( "%s %s at %s total %s", side, amt / price, price, amt);
		return postOrderToObj( createOrder2(side, amt / price, price) );
	}

	public void testFracShares()  throws Exception {
		JsonObject obj = createOrder("BUY", 1.5, 2); 
		JsonObject map = postOrderToObj(obj);
		S.out( "testFracShares " + map);
		assert200();
		assertEquals(RefCode.OK, cli.getRefCode() );
		
		JsonObject ret = getLiveMessage(map.getString("id"));
		assertEquals( "message", ret.getString("type") );
		startsWith( "Bought 1.50", ret.getString("text") );
	}

	public void testSmallOrder()  throws Exception {  // no order should be submitted to exchange
		JsonObject obj = createOrder("BUY", .4, 2); 
		JsonObject map = postOrderToObj(obj);

		assert200();
		assertEquals(RefCode.OK, cli.getRefCode() );

		JsonObject ret = getLiveMessage(map.getString("id"));
		assertEquals( "message", ret.getString("type") ); // this sometimes fails because of stale data
		startsWith( "Bought 0.4", ret.getString("text") );
	}

	public void testZeroShares()  throws Exception {
		JsonObject obj = createOrder3("{ 'msg': 'order', 'conid': '265598', 'action': 'buy', 'quantity': '0', 'tokenPrice': '138' }", false); 
		JsonObject map = postOrderToObj(obj);
		String ret = map.getString( "code");
		String text = map.getString("message");
		S.out( "zero shares: " + text);
		assertEquals( "Quantity must be positive", text);
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
	}
	
	/** This only works after hours and with a contract that hasn't traded for five minutes */
//	public void testNotRecent() throws Exception {
//		JsonObject obj = createOrder3("{ 'msg': 'order', 'conid': '265768', 'action': 'buy', 'quantity': '.1', 'tokenPrice': '600' }"); 
//		JsonObject map = postOrderToObj(obj);
//		assertEquals( 200, cli.getResponseCode() );
//		S.sleep(100); 
//		JsonObject resp = getLiveMessage2(map.getString("id"));
//		assertEquals( RefCode.STALE_DATA.toString(), resp.getString("errorCode") );
//	}
	
	/** no Fireblocks */
	static JsonObject createOrder(String side, double qty, double offset) throws Exception {
		return createOrder2( side, qty, curPrice + offset, false);
	}

	/** no Fireblocks */
	static JsonObject createOrder2(String side, double qty, double price) throws Exception {
		return createOrder2( side, qty, price, false);
	}
	
	static JsonObject createOrder2(String side, double qty, double price, boolean fireblocks) throws Exception {
		String json = String.format( "{ 'conid': '265598', 'action': '%s', 'quantity': %s, 'tokenPrice': '%s' }",
				side, qty, price);
		return createOrder3(json, fireblocks);
	}
	
	static JsonObject createOrder3(String json, boolean fireblocks) throws Exception {
		JsonObject obj = JsonObject.parse( Util.easyJson(json) );
		obj.put("cookie", Cookie.cookie);
		obj.put("currency", m_config.rusd().name() );
		obj.put("wallet_public_key", Cookie.wallet);
		obj.put("testcase", true);

		if (!fireblocks) {
			obj.put("noFireblocks", true);
		}
		
		double price = obj.getDouble("tokenPrice");
		double qty = obj.getDouble("quantity");
		double amt = price * qty;
		boolean buy = obj.getString("action").equalsIgnoreCase("BUY");
		
		double tds = buy
				? 0
				: (amt - m_config.commission() ) * .01;
		obj.put("tds", tds);
		
		double extra = m_config.commission() + tds;
		
		double total = buy ? amt + extra : amt - extra;
		obj.put("amount", total);
		
		return obj;
	}
}
