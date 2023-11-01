package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import http.MyHttpClient;
import reflection.Main;
import reflection.Prices;
import reflection.RefCode;
import tw.util.S;

public class TestOrder extends MyTestCase {
	static double curPrice;
	static boolean m_noFireblocks = true;
	
//	static double approved;
	
	static {
		try {
			String conid = "265598";
			
			curPrice = m_config.newRedis().singleQuery( 
					jedis -> {
						String bid = jedis.hget(conid, "bid");
						String ask = jedis.hget(conid, "ask");
						if (S.isNull(bid) || S.isNull(ask) ) {
							jedis.hset( conid, "bid", "138.2");
							jedis.hset( conid, "ask", "138.4");
							return 138.3;
						}
						return Double.valueOf(bid);
					});
			S.out( "TestOrder: Current AAPL price is %s", curPrice);
		//	approved = config.busd().getAllowance(Cookie.wallet, config.rusdAddr() );
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	


	// verify user record
	public void testMissingUserRec() throws Exception {
		m_config.sqlCommand( conn -> conn.delete("delete from users where wallet_public_key = '%s'", dead) );
		
		JsonObject obj = createOrder("BUY", 10, 2);
		obj.put("wallet_public_key", dead);
		postOrderToObj(obj);
		assertEquals( RefCode.INVALID_USER_PROFILE, cli.getRefCode() );
		
		String[] fields = {
				"wallet_public_key", "first_name", "last_name", "email", "phone", "aadhaar", 
		};
		Object[] vals = {
				dead, "bob", "jones", "a@b.com", "9143933732", "my adhaar",
		};
		m_config.sqlCommand( conn -> conn.insert("users", fields, vals) );
		
		obj = createOrder("BUY", 10, 2);
		obj.put("wallet_public_key", dead);
		postOrderToObj(obj);
		assertEquals( RefCode.INVALID_USER_PROFILE, cli.getRefCode() );  // missing pan
		
		m_config.sqlCommand( conn -> conn.execute( String.format("update users set pan_number = 'abc' where wallet_public_key = '%s'", dead) ) );
		
		obj = createOrder("BUY", 10, 2);
		obj.put("wallet_public_key", dead);
		postOrderToObj(obj);
		assertEquals( RefCode.INVALID_USER_PROFILE, cli.getRefCode() );  // invalid pan, aadhaar
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

	/** Test that order failes if the siwe session has timed out */
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
		JsonObject obj = TestOrder.createOrder( "BUY", 10, 3);
		
		// this won't work because you have to 
		//obj.remove("noFireblocks"); // let the fireblocks go through so we can test the crypto_transaction
		
		JsonObject map = postOrderToObj(obj);
		assert200();
		assertEquals( RefCode.OK, cli.getRefCode() );
		
		JsonObject ret = getLiveMessage(map.getString("id"));
		assertEquals( "message", ret.getString("type") );
		startsWith( "Bought 10", ret.getString("text") );
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
		JsonObject obj = createOrder( "sell", 10, -3);
		JsonObject map = postOrderToObj(obj);
		String code = map.getString( "code");
		String text = map.getString("message");
		S.out( "fillSell %s %s", code, text);
		assert200();
		assertEquals(RefCode.OK, cli.getRefCode() );

		JsonObject ret = getLiveMessage(map.getString("id"));
		assertEquals( "message", ret.getString("type") );
		startsWith( "Sold 10", ret.getString("text") );
	}

	public void testMinOrderSize() throws Exception {
		double qty = m_config.minOrderSize() / curPrice - .01;
		JsonObject map = postOrderToObj( createOrder2("buy", qty, 138) );
		String ret = map.getString( "code");
		assertEquals( RefCode.ORDER_TOO_SMALL.toString(), ret);
	}
	
	public void testMaxAmtBuy()  throws Exception {
		double qty = m_config.maxBuyAmt() / curPrice + 1;
		JsonObject map = postOrderToObj( createOrder2("buy", qty, curPrice) );
		String ret = map.getString( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);
	}

	public void testMaxAmtSell()  throws Exception {
		double qty = m_config.maxSellAmt() / 138 + 1;
		JsonObject obj = createOrder2("buy", qty, 138);
		JsonObject map = postOrderToObj(obj);
		String ret = map.getString( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);
	}

	public void testKyc()  throws Exception {
		m_config.sqlCommand( sql -> sql.delete("delete kyc_status from users where wallet_public_key = '%s'", Cookie.wallet) );
		double qty = m_config.nonKycMaxOrderSize() / 138 + 1;
		JsonObject obj = createOrder2("buy", qty, 138);
		
		JsonObject map = postOrderToObj(obj);
		assertEquals( RefCode.NEED_KYC, cli.getRefCode() );
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
		JsonObject obj = createOrder3("{ 'msg': 'order', 'conid': '265598', 'action': 'buy', 'quantity': '0', 'tokenPrice': '138' }"); 
		JsonObject map = postOrderToObj(obj);
		String ret = map.getString( "code");
		String text = map.getString("message");
		S.out( "zero shares: " + text);
		assertEquals( "Quantity must be positive", text);
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
	}
	
	/** This only works after hours and with a contract that hasn't traded for five minutes */
	public void testNotRecent() throws Exception {
		JsonObject obj = createOrder3("{ 'msg': 'order', 'conid': '265768', 'action': 'buy', 'quantity': '.1', 'tokenPrice': '600' }"); 
		JsonObject map = postOrderToObj(obj);
		assertEquals( 200, cli.getResponseCode() );
		S.sleep(100); 
		JsonObject resp = getLiveMessage2(map.getString("id"));
		assertEquals( RefCode.STALE_DATA.toString(), resp.getString("errorCode") );
	}
	
	static JsonObject createOrder(String side, double qty, double offset) throws Exception {
		return createOrder2( side, qty, curPrice + offset);
	}
	
	static JsonObject createOrder2(String side, double qty, double price) throws Exception {
		String json = String.format( "{ 'conid': '265598', 'action': '%s', 'quantity': %s, 'tokenPrice': '%s' }",
				side, qty, price);
		return createOrder3(json);
	}
	
	static JsonObject createOrder3(String json) throws Exception {
		JsonObject obj = JsonObject.parse( Util.fmtJson(json) );
		obj.put("cookie", Cookie.cookie);
		obj.put("currency", "USDC");
		obj.put("wallet_public_key", Cookie.wallet);
		obj.put("noFireblocks", true);
		obj.put("testcase", true);
		
		double price = obj.getDouble("tokenPrice");
		double qty = obj.getDouble("quantity");
		double amt = price * qty;
		boolean buy = obj.getString("action").equalsIgnoreCase("BUY");
		
		double tds = buy
				? amt * .01
				: (amt - m_config.commission() ) * .01;
		obj.put("tds", tds);
		
		double total = buy ? amt + m_config.commission() : amt - m_config.commission() - tds;
		obj.put("amount", total);
		
		return obj;
	}
}
