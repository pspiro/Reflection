package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import http.MyHttpClient;
import reflection.Main;
import reflection.Prices;
import reflection.RefCode;
import tw.util.S;
import web3.StockToken;

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
	
			createValidUser();
			
//			S.out( "minting 5000 RUSD");
//			m_config.rusd().mintRusd( Cookie.wallet, 5000, stocks.getAnyStockToken() )
//				.waitForHash();
//			waitForRusdBalance(Cookie.wallet, 5000, false);

			//	approved = config.busd().getAllowance(Cookie.wallet, config.rusdAddr() );
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	/** Create user with valid profile and passed KYC */
	static void createValidUser() throws Exception {
		JsonObject json = TestProfile.createProfileNC();
		json.put( "kyc_status", "VERIFIED");
		
		m_config.sqlCommand( conn -> conn.insertOrUpdate(
				"users", 
				json, 
				"wallet_public_key = '%s'",
				Cookie.wallet.toLowerCase() ) );
	}

	


	// test missing user rec; profile fields are tested in testProfile
	public void testMissingUserRec() throws Exception {
		// test missing user rec
		m_config.sqlCommand( conn -> conn.delete("delete from users where wallet_public_key = '%s'", Cookie.wallet.toLowerCase()) );

		JsonObject obj = createOrderWithOffset("BUY", 10, 2);
		postOrderToObj(obj);
		assertEquals( RefCode.INVALID_USER_PROFILE, cli.getRefCode() );

		createValidUser();
	}
	
	// missing walletId
	public void testMissingWallet() throws Exception {
		JsonObject obj = createOrderWithOffset("BUY", 10, 2);
		obj.remove("wallet_public_key");
		JsonObject map = postOrderToObj(obj);
		String ret = map.getString( "code");
		String text = map.getString("message");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( text, "Param 'wallet_public_key' is missing");
	}
	
	// reject order; price too low
	public void testBuyTooLow() throws Exception {
		JsonObject obj = createOrderWithOffset( "BUY", 10, -1);
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
		JsonObject obj = createOrderWithOffset( "SELL", 10, 1);
		JsonObject map = postOrderToObj(obj);
		String code = map.getString( "code");
		String text = map.getString("message");
		S.out("sellTooHigh %s %s", code, text);
		assertEquals( RefCode.INVALID_PRICE.toString(), code);
		assertEquals( Prices.TOO_HIGH, text);
	}
	
	public void testTransTableEntry() throws Exception {
		JsonObject obj = createOrderWithOffset( "BUY", 10, -1);
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
			
			postOrderToObj( TestOrder.createOrderWithOffset( "BUY", 10, 3) );
			assertEquals( RefCode.VALIDATION_FAILED, cli.getRefCode() ); 
		});
	}

	// fill order buy order
	public void testFillBuy() throws Exception {
		StockToken stockToken = stocks.getStockByConid(265598).getToken();
		
		S.out( "pos: " + stockToken.getPosition( Cookie.wallet) );

		// buy 10
		postOrderToObj( TestOrder.createOrderWithOffset( "BUY", 10, 3) );  // try again w/ autofill off
		assert200();
		assertEquals( RefCode.OK, cli.getRefCode() );
		S.out( "received %s %s", cli.getRefCode(), cli.getMessage() );

		// wait for refapi to return status filled which comes from FbServer in Fireblocks mode
		String text = waitForFilled( cli.getUId() );
		startsWith( "Bought 10", text);
		
		S.out( "pos: " + stockToken.getPosition( Cookie.wallet) );

		// wait for the position to register (tests HookServer)
		waitForBalance( 
				Cookie.wallet, 
				stockToken.address(), 
				10, 
				false);  

		S.out( "pos: " + stockToken.getPosition( Cookie.wallet) );

		// sell 2
		postOrderToObj( createOrderWithOffset( "sell", 2, -3) );
		assert200();
		assertEquals(RefCode.OK, cli.getRefCode() );
		
		// wait for refapi to return status filled which comes from FbServer in Fireblocks mode
		text = waitForFilled( cli.getUId() );
		startsWith( "Sold 2", text);
		
		// wait for the position to register (tests HookServer)
		waitForBalance( 
				Cookie.wallet, 
				stockToken.address(), 
				10, 
				false);  
	}

	/** When the IB order is filled, we get a message back saying
	 *  the order is being processed on the blockchain */
	public void testToast() throws Exception {
		JsonObject ord = TestOrder.createOrderWithOffset( "BUY", 1, 3);
		ord.remove( "noFireblocks");
		postOrderToObj( ord );  // try again w/ autofill off
		assert200();
		
		S.sleep(50);
		JsonObject ret = getLiveMessage( cli.getUId() );
		assertEquals( "message", ret.getString("type") );
		startsWith( "Your order was filled", ret.getString("text") );
	}

	public void testNullCookie() throws Exception {
		JsonObject obj = createOrderWithOffset( "BUY", 10, 3);
		obj.remove("cookie");
		
		MyHttpClient cli = postOrder(obj);
		JsonObject map = cli.readJsonObject();
		String text = map.getString("message");
		assertEquals( 400, cli.getResponseCode() );
		startsWith( "Null cookie", text);
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
		// mint some if needed
		if (m_config.rusd().getPosition( Cookie.wallet) < m_config.maxOrderSize() + 10) {
			m_config.rusd().mintRusd( Cookie.wallet, m_config.maxOrderSize() + 10, stocks.getAnyStockToken() )
				.waitForHash();
			waitForRusdBalance(Cookie.wallet, 5000, false);
		}

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
	}
	
	private JsonObject placeOrder( String side, double amt, double price) throws Exception {
		S.out( "%s %s at %s total %s", side, amt / price, price, amt);
		return postOrderToObj( createOrderWithPrice(side, amt / price, price) );
	}

	public void testFracShares()  throws Exception {
		JsonObject obj = createOrderWithOffset("BUY", 1.6, 2); 
		JsonObject map = postOrderToObj(obj);
		S.out( "testFracShares " + map);
		assert200();
		
		String text = waitForFilled( map.getString("id") );
		startsWith( "Bought 1.6", text);
	}

	// failing
	public void testSmallOrder()  throws Exception {  // no order should be submitted to exchange
		JsonObject obj = createOrderWithOffset("BUY", .4, 2); 
		JsonObject map = postOrderToObj(obj);
		assert200();
		
		String text = waitForFilled( map.getString("id") );
		startsWith( "Bought .4", text);
	}

	public void testZeroShares()  throws Exception {
		JsonObject obj = createOrder4("{ 'msg': 'order', 'conid': '265598', 'action': 'buy', 'quantity': '0', 'tokenPrice': '138' }", "RUSD"); 
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
	
	static JsonObject createOrderWithOffset(String side, double qty, double offset) throws Exception {
		return createOrder3( side, qty, curPrice + offset, "RUSD");
	}

	static JsonObject createOrderWithPrice(String side, double qty, double price) throws Exception {
		return createOrder3( side, qty, price, "RUSD");
	}
	
	static JsonObject createOrder3(String side, double qty, double price, String currency) throws Exception {
		String json = String.format( "{ 'conid': '265598', 'action': '%s', 'quantity': %s, 'tokenPrice': '%s' }",
				side, qty, price);
		return createOrder4(json, currency);
	}
	
	static JsonObject createOrder4(String json, String currency) throws Exception {
		JsonObject obj = JsonObject.parse( Util.easyJson(json) );
		obj.put("cookie", Cookie.cookie);
		obj.put("currency", currency);
		obj.put("wallet_public_key", Cookie.wallet);
		obj.put("testcase", true);

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
