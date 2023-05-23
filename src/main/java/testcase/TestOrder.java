package testcase;

import http.MyHttpClient;
import json.MyJsonObject;
import reflection.Prices;
import reflection.RefCode;
import reflection.Util;
import tw.util.S;

public class TestOrder extends MyTestCase {
	static double curPrice;
	static boolean m_noFireblocks = true;
//	static double approved;
	
	static {
		try {
			curPrice = m_config.newRedis().singleQuery( 
					jedis -> Double.valueOf( jedis.hget("265598", "bid") ) );
			S.out( "TestOrder: Current IBM price is %s", curPrice);
		//	approved = config.busd().getAllowance(Cookie.wallet, config.rusdAddr() );
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	// missing walletId
	public void testMissingWallet() throws Exception {
		MyJsonObject obj = orderData("BUY", 10, 2);
		obj.remove("wallet_public_key");
		MyJsonObject map = postDataToObj(obj);
		String ret = map.getString( "code");
		String text = map.getString("message");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( text, "Param 'wallet_public_key' is missing");
	}
	
	// reject order; price too high; IB won't accept it
	public void testBuyTooHigh() throws Exception {
		MyJsonObject obj = orderData("{ 'msg': 'order', 'conid': '265598', 'action': 'buy', 'quantity': '10', 'tokenPrice': '200', 'cryptoid': 'testmaxamtbuy' }");		
		MyJsonObject map = postDataToObj(obj);
		String code = map.getString( "code");
		String text = map.getString("message");
		S.out( "testOrder4: %s", map);
		assertEquals( RefCode.REJECTED.toString(), code);  // fails if auto-fill is on
		assertEquals( "Reason unknown", text);
	}
	
	// reject order; price too low
	public void testBuyTooLow() throws Exception {
		MyJsonObject obj = orderData( "BUY", 10, -1);
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
		MyJsonObject obj = orderData( "SELL", 10, 1);
		MyJsonObject map = postDataToObj(obj);
		String code = map.getString( "code");
		String text = map.getString("message");
		S.out("sellTooHigh %s %s", code, text);
		assertEquals( RefCode.INVALID_PRICE.toString(), code);
		assertEquals( Prices.TOO_HIGH, text);
	}
	
	// reject order; sell price too low; IB rejects it
	public void testSellPriceTooLow() throws Exception {
		MyJsonObject obj = orderData( "SELL", 100, -30);
		MyJsonObject map = postDataToObj(obj);
		String code = map.getString( "code");
		String text = map.getString("message");
		S.out("sell too low %s %s", code, text);
		assertEquals( RefCode.INVALID_PRICE.toString(), code);  // test fails if autoFill is on
		assertEquals( Prices.TOO_LOW, text);
	}

	// fill order buy order
	public void testFillBuy() throws Exception {
		MyJsonObject obj = TestOrder.orderData( "BUY", 10, 3);
		
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
		MyJsonObject obj = orderData( "BUY", 10, 3);
		obj.remove("cookie");
		
		MyHttpClient cli = postData(obj);
		MyJsonObject map = cli.readMyJsonObject();
		String text = map.getString("message");
		assertEquals( 400, cli.getResponseCode() );
		startsWith( "Null cookie", text);
	}
	
	// fill order sell order
	public void testFillSell() throws Exception {
		MyJsonObject obj = orderData( "sell", 10, -3);
		MyJsonObject map = postDataToObj(obj);
		String code = map.getString( "code");
		String text = map.getString("message");
		S.out( "fillSell %s %s", code, text);
		assertEquals( RefCode.OK.toString(), code);
		double filled = map.getDouble( "filled");
		assertEquals( 10.0, filled);
	}
	
	public void testMaxAmtBuy()  throws Exception {
		MyJsonObject obj = orderData("{ 'msg': 'order', 'conid': '265598', 'action': 'buy', 'quantity': '200', 'tokenPrice': '138', 'cryptoid': 'testmaxamtbuy' }");
		MyJsonObject map = postDataToObj(obj);
		String ret = map.getString( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);
	}

	public void testMaxAmtSell()  throws Exception {
		MyJsonObject obj = orderData("{ 'msg': 'order', 'conid': '265598', 'action': 'sell', 'quantity': '200', 'tokenPrice': '138', 'cryptoid': 'testmaxamtsell' }"); 
		MyJsonObject map = postDataToObj(obj);
		String ret = map.getString( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);
	}

	public void testFracShares()  throws Exception {
		MyJsonObject obj = orderData("BUY", 1.5, 2); 
		MyJsonObject map = postDataToObj(obj);
		S.out( "testFracShares " + map.toString() ); 
		assertEquals( RefCode.OK.toString(), map.getString( "code") );
		assertEquals( 1.5, map.getDouble( "filled") );
	}

	public void testSmallOrder()  throws Exception {  // no order should be submitted to exchange
		MyJsonObject obj = orderData("BUY", .4, 2); 
		MyJsonObject map = postDataToObj(obj);
		assertEquals( RefCode.OK.toString(), map.getString( "code") );
		assertEquals( .4, map.getDouble( "filled") );
	}

	public void testZeroShares()  throws Exception {
		MyJsonObject obj = orderData("{ 'msg': 'order', 'conid': '265598', 'action': 'buy', 'quantity': '0', 'tokenPrice': '138' }"); 
		MyJsonObject map = postDataToObj(obj);
		String ret = map.getString( "code");
		String text = map.getString("message");
		S.out( "zero shares: " + text);
		assertEquals( "Quantity must be positive", text);
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
	}
	
	static MyJsonObject orderData(String json) throws Exception {
		MyJsonObject obj = MyJsonObject.parse( Util.toJson(json) );
		addRequiredFields(obj);
		return obj;
	}
	
	static MyJsonObject orderData(String side, double qty, double offset) throws Exception {
		return orderData2( side, qty, curPrice + offset);
	}
	
	static MyJsonObject orderData2(String side, double qty, double price) throws Exception {
		String json = String.format( "{ 'conid': '265598', 'action': '%s', 'quantity': %s, 'tokenPrice': '%s' }",
				side, qty, price);
		MyJsonObject obj = MyJsonObject.parse( Util.toJson(json) );
		addRequiredFields(obj);
		return obj;
	}
	
	static MyJsonObject addRequiredFields(MyJsonObject obj) throws Exception {
		obj.put("cookie", Cookie.cookie);
		obj.put("currency", "USDC");
		obj.put("wallet_public_key", Cookie.wallet);
		obj.put("noFireblocks", true);
		
		double price = obj.getDouble("tokenPrice");
		double qty = obj.getDouble("quantity");
		double amt = price * qty;
		boolean buy = obj.getString("action").equalsIgnoreCase("BUY");
		
		double tds = (amt - m_config.commission() ) * .01;
		if (!buy) {
			obj.put("tds", tds);
		}
		
		double total = buy ? amt + m_config.commission() : amt - m_config.commission() - tds;
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
