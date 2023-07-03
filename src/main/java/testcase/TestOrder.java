package testcase;

import org.json.simple.JsonObject;

import http.MyHttpClient;
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
			S.out( "TestOrder: Current AAPL price is %s", curPrice);
		//	approved = config.busd().getAllowance(Cookie.wallet, config.rusdAddr() );
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

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
	
	// reject order; price too high; IB won't accept it
	public void testBuyTooHigh() throws Exception {
		JsonObject obj = orderData("{ 'msg': 'order', 'conid': '265598', 'action': 'buy', 'quantity': '10', 'tokenPrice': '200', 'cryptoid': 'testmaxamtbuy' }");		
		JsonObject map = postOrderToObj(obj);
		String code = map.getString( "code");
		String text = map.getString("message");
		S.out( "testOrder4: %s", map);
		assertEquals( RefCode.REJECTED.toString(), code);  // fails if auto-fill is on
		assertEquals( "Reason unknown", text);
		
		assertEquals(200, cli.getResponseCode() );
		assertEquals(RefCode.OK, cli.getCode() );

		JsonObject ret = getLiveMessage(map.getString("id"));
		assertEquals( "message", ret.getString("type") );
		startsWith( "Sold 10", ret.getString("text") );
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
	
	// reject order; sell price too low; IB rejects it
	public void testSellPriceTooLow() throws Exception {
		JsonObject obj = createOrder( "SELL", 100, -30);
		JsonObject map = postOrderToObj(obj);
		String code = map.getString( "code");
		String text = map.getString("message");
		S.out("sell too low %s %s", code, text);
		assertEquals( RefCode.INVALID_PRICE.toString(), code);  // test fails if autoFill is on
		assertEquals( Prices.TOO_LOW, text);
	}

	// fill order buy order
	public void testFillBuy() throws Exception {
		JsonObject obj = TestOrder.createOrder( "BUY", 10, 3);
		
		// this won't work because you have to 
		//obj.remove("noFireblocks"); // let the fireblocks go through so we can test the crypto_transaction
		
		JsonObject map = postOrderToObj(obj);
		assertEquals( 200, cli.getResponseCode() );
		assertEquals( RefCode.OK, cli.getCode() );
		
		JsonObject ret = getLiveMessage(map.getString("id"));
		assertEquals( "message", ret.getString("type") );
		startsWith( "Bought 10", ret.getString("text") );
	}

	public void testNullCookie() throws Exception {
		JsonObject obj = createOrder( "BUY", 10, 3);
		obj.remove("cookie");
		
		MyHttpClient cli = postOrder(obj);
		JsonObject map = cli.readMyJsonObject();
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
		assertEquals(200, cli.getResponseCode() );
		assertEquals(RefCode.OK, cli.getCode() );

		JsonObject ret = getLiveMessage(map.getString("id"));
		assertEquals( "message", ret.getString("type") );
		startsWith( "Sold 10", ret.getString("text") );
	}
	
	public void testMaxAmtBuy()  throws Exception {
		JsonObject obj = orderData("{ 'msg': 'order', 'conid': '265598', 'action': 'buy', 'quantity': '1000', 'tokenPrice': '138', 'cryptoid': 'testmaxamtbuy' }");
		JsonObject map = postOrderToObj(obj);
		String ret = map.getString( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);
	}

	public void testMaxAmtSell()  throws Exception {
		JsonObject obj = orderData("{ 'msg': 'order', 'conid': '265598', 'action': 'sell', 'quantity': '1000', 'tokenPrice': '138', 'cryptoid': 'testmaxamtsell' }"); 
		JsonObject map = postOrderToObj(obj);
		String ret = map.getString( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);
	}

	public void testFracShares()  throws Exception {
		JsonObject obj = createOrder("BUY", 1.5, 2); 
		JsonObject map = postOrderToObj(obj);
		assertEquals(200, cli.getResponseCode() );
		assertEquals(RefCode.OK, cli.getCode() );

		JsonObject ret = getLiveMessage(map.getString("id"));
		assertEquals( "message", ret.getString("type") );
		startsWith( "Bought 1.50", ret.getString("text") );
	}

	public void testSmallOrder()  throws Exception {  // no order should be submitted to exchange
		JsonObject obj = createOrder("BUY", .4, 2); 
		JsonObject map = postOrderToObj(obj);

		assertEquals(200, cli.getResponseCode() );
		assertEquals(RefCode.OK, cli.getCode() );

		JsonObject ret = getLiveMessage(map.getString("id"));
		assertEquals( "message", ret.getString("type") );
		startsWith( "Bought 0.4", ret.getString("text") );
	}

	public void testZeroShares()  throws Exception {
		JsonObject obj = orderData("{ 'msg': 'order', 'conid': '265598', 'action': 'buy', 'quantity': '0', 'tokenPrice': '138' }"); 
		JsonObject map = postOrderToObj(obj);
		String ret = map.getString( "code");
		String text = map.getString("message");
		S.out( "zero shares: " + text);
		assertEquals( "Quantity must be positive", text);
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
	}
	
	static JsonObject orderData(String json) throws Exception {
		JsonObject obj = JsonObject.parse( Util.toJson(json) );
		addRequiredFields(obj);
		return obj;
	}
	
	static JsonObject createOrder(String side, double qty, double offset) throws Exception {
		return createOrder2( side, qty, curPrice + offset);
	}
	
	static JsonObject createOrder2(String side, double qty, double price) throws Exception {
		String json = String.format( "{ 'conid': '265598', 'action': '%s', 'quantity': %s, 'tokenPrice': '%s' }",
				side, qty, price);
		JsonObject obj = JsonObject.parse( Util.toJson(json) );
		addRequiredFields(obj);
		return obj;
	}
	
	static JsonObject addRequiredFields(JsonObject obj) throws Exception {
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
}