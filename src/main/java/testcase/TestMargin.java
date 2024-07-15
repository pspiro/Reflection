package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import reflection.MarginOrder.Status;
import reflection.MarginTrans;
import reflection.RefCode;
import tw.util.S;

public class TestMargin extends MyTestCase {
	static double base = TestOrder.curPrice;
	static String conid = "" + TestOrder.conid;
	
//	server.createContext("/api/margin-order", exch -> new MarginTrans(this, exch, true).marginOrder() );
//	server.createContext("/api/margin-cancel", exch -> new MarginTrans(this, exch, true).marginCancel() );
//	server.createContext("/api/margin-update", exch -> new MarginTrans(this, exch, true).marginUpdate() );
//	server.createContext("/api/margin-get-order", exch -> new MarginTrans(this, exch, true).marginGetOrder() );
//	server.createContext("/api/margin-get-all", exch -> new MarginTrans(this, exch, true).marginGetAll() );
//	server.createContext("/api/margin-liquidate", exch -> new MarginTrans(this, exch, true).marginLiquidate() );
//	server.createContext("/api/margin-add-funds", exch -> new MarginTrans(this, exch, true).marginAddFunds() );
//	server.createContext("/api/margin-withdraw-funds", exch -> new MarginTrans(this, exch, true).marginWithdrawFunds() );
//	server.createContext("/api/margin-withdraw-tokens", exch -> new MarginTrans(this, exch, true).marginWithdrawTokens() );
//	server.createContext("/api/margin-info", exch -> new MarginTrans(this, exch, true).marginInfo() );
	
	static {
		try {
			if (m_config.rusd().getPosition(Cookie.wallet) < 1000) {
				m_config.rusd().mintRusd(Cookie.wallet, 100000, stocks.getAnyStockToken() )
					.displayHash();
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void testStaticQuery() throws Exception {
		cli().get("/api/margin-static/" + Cookie.wallet).readJsonObject().display(); // cookie is not required but Frontend should pass it for debugging
		assert200();
	}

	public void testDynamicQuery() throws Exception {
		// fail missing conid
		cli().postToJson( "/api/margin-dynamic", Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie
				) )
			.display();
		failWith( RefCode.INVALID_REQUEST);
		assertStartsWith( "Param 'conid'", cli.getMessage() );

		// fail missing cookie
		cli().postToJson( "/api/margin-dynamic", Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"conid", conid
				) )
			.display();
		failWith( RefCode.VALIDATION_FAILED);

		// success
		cli().postToJson( "/api/margin-dynamic", Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"conid", conid,
				"cookie", Cookie.cookie
				) )
			.display();
		assert200();
	}

	public void testFailOrder() throws Exception {
		// fail user profile
		String prev = Cookie.wallet;
		Cookie.setNewFakeAddress( false);
		cli().postToJson( "/api/margin-order", newOrd() );
		failWith( RefCode.INVALID_USER_PROFILE);
		Cookie.setWalletAddr(prev);
		
		// fail no cookie
		JsonObject json = newOrd();
		json.remove( "cookie");
		cli().postToJson( "/api/margin-order", json);
		failWith( RefCode.VALIDATION_FAILED);
		
		// fail amt too high
		json = newOrd();
		json.put( "amountToSpend", 1000000.0);
		cli().postToJson( "/api/margin-order", json);
		failWith( RefCode.INVALID_REQUEST, "The amount to spend");

		// leverage too high
		json = newOrd();
		json.put( "leverage", 200.0);
		cli().postToJson( "/api/margin-order", json);
		failWith( RefCode.INVALID_REQUEST, "Leverage");
	}
	
	public void testGetOrder() throws Exception {
		// place order
		JsonObject json = cli().postToJson( "/api/margin-order", newOrd() );
		assert200();
		
		// fail invalid order id
		cli().getToJson("/api/margin-get-order/" + json.getString( "abc") ).display();
		assert400();
		
		// fail no such order
		cli().getToJson("/api/margin-get-order/" + json.getString( "abcdeghij") ).display();
		assert400();
		
		// success
		cli().getToJson("/api/margin-get-order/" + json.getString( "orderId") ).display();
		assert200();
		
		// cancel order
		cli().postToJson( "/api/margin-cancel",	Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"orderId", json.getString( "orderId") ) );
	}	

	public void testBuyNoFill() throws Exception {
		S.out( "placing order");
		JsonObject json = cli().postToJson( "/api/margin-order", newOrd() );
		json.display();
		assert200();


		// retrieve the order
		cli().getToJson("/api/margin-get-order/" + json.getString( "orderId") ).display();
		assert200();

		S.out( "wait to accept pmt and place buy order");
		int i = Util.waitFor(40, () -> {
			return cli().getToJson("/api/margin-get-order/" + json.getString( "orderId") ) 
					.getString( "status").equals( "PlacedBuyOrder");
		});
		
		//cancel( json.getString("orderId") );
		
		assertTrue( i >= 0);
	}

	public void testFillBuyOnly() throws Exception {
		JsonObject ord = newOrd();
		ord.put( "profitTakerPrice", base + 2);
		ord.put( "entryPrice", base + 1);
		ord.put( "stopLossPrice", base - 1);

		JsonObject json = cli().postToJson( "/api/margin-order", ord );
		json.display();
		assert200();

		S.out( "wait 5 sec to accept pmt and fill buy order");
		int i = Util.waitFor(40, () -> {
			String status = queryDynamic().find( "orderId", json.getString("orderId") )
					.getString( "status");
			return status.equals( "BuyOrderFilled") || status.equals( "PlacedSellOrders");
		});
		
//		cancel( json.getString("orderId") );
		
		assertTrue( i >= 0);
	}

	public void testFillBuyAndStop() throws Exception {
		JsonObject ord = newOrd();
		ord.put( "profitTakerPrice", base + 3);
		ord.put( "entryPrice", base + 2);
		ord.put( "stopLossPrice", base + 1);
		
		S.out( "placing order");
		JsonObject json = cli().postToJson( "/api/margin-order", ord );
		assert200();
		
		S.out( "wait 5 sec to fill");
		S.sleep( 5000);
		
		JsonObject live = queryDynamic().find( "orderId", json.getString("orderId") );
		assertEquals( Status.Completed, live.getString( "status") );
	}
	
	public void testUpdate() throws Exception {
		S.out( "placing order");
		JsonObject json = cli().postToJson( "/api/margin-order", newOrd() );
		assert200();

		// fail no cookie
		cli().postToJson( "/api/margin-update", Util.toJson(
				"orderId", json.getString( "orderId"),
				"profitTakerPrice", base + 3,
				"entryPrice", base + 2,
				"stopLossPrice", base + 1) );

		// fail wrong order id
		cli().postToJson( "/api/margin-update", Util.toJson(
				"cookie", Cookie.cookie,
				"orderId", "lkjsdflksjdf",
				"profitTakerPrice", base + 3,
				"entryPrice", base + 2,
				"stopLossPrice", base + 1) );

		// fail entry price has been increased
		cli().postToJson( "/api/margin-update", Util.toJson(
				"cookie", Cookie.cookie,
				"orderId", json.getString( "orderId"),
				"profitTakerPrice", base + 3,
				"entryPrice", base,
				"stopLossPrice", base + 1) );

		// succeed
		cli().postToJson( "/api/margin-update", Util.toJson(
				"cookie", Cookie.cookie,
				"orderId", json.getString( "orderId"),
				"profitTakerPrice", base + 3,
				"entryPrice", base + 2,
				"stopLossPrice", base + 1) );
	}
	
	public void testLiquidate() {
	}

	public void testAddFunds() {
	}

	public void testWithdrawFunds() {
		
	}

	public void testWithdrawTokens() {
	}


	public void testCancel() throws Exception {
		S.out( "placing order");
		JsonObject json = cli().postToJson( "/api/margin-order", newOrd() );
		assert200();
		assertEquals( 10, json.getString( "orderId").length() );
		json.display();

		// find order in query
		JsonObject dynamic = cli().postToJson( "/api/margin-dynamic", Util.toJson(
				"wallet_public_key", Cookie.wallet, 
				"cookie", Cookie.cookie,
				"conid", conid) );
		
		JsonObject live = dynamic.getArray( "orders").find( "orderId", json.getString("orderId") );
		assertTrue( "live order not found", live != null);
		live.display();
		
		// missing orderId
		cli().postToJson( "/api/margin-cancel",	Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie) )
			.display();
		failWith( RefCode.INVALID_REQUEST);
		assertStartsWith( "Param 'orderId'", cli.getMessage() );

		// fail wrong orderId
		cli().postToJson( "/api/margin-cancel",	Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"orderId", "myorderid",
				"cookie", Cookie.cookie) )
			.display();
		failWith( RefCode.INVALID_REQUEST);
		
		// fail wrong wallet
		cli().postToJson( "/api/margin-cancel",	Util.toJson( 
				"wallet_public_key", dead,
				"orderId", "myorderid",
				"cookie", Cookie.cookie) )
			.display();
		
		// cancel, success
		cancel( json.getString("orderId") );

		// fail already canceled
		cancel( json.getString("orderId") );
	}
	
	void cancel(String orderId) throws Exception {
		showStatus( orderId);

		cli().postToJson( "/api/margin-cancel",	Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"orderId", orderId,
				"cookie", Cookie.cookie
				) );
		assert200();
	}
	static JsonObject newOrd() {
		return Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"conid", conid,
				"amountToSpend", 100,
				"leverage", 1.,
				"profitTakerPrice", base + 1,
				"entryPrice", base - 1,
				"stopLossPrice", base - 2,
				"goodUntil", "EndOfDay",
				"currency", "RUSD"
				);
	}
	
	private void showStatus(String id) throws Exception {
		JsonArray ords = cli().postToJson( "/api/margin-dynamic", Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"conid", conid,
				"cookie", Cookie.cookie
				) ).getArray( "orders");

		Util.iff( ords.find( "orderId", id), ord -> S.out( ord) );
	}
	
	/** Return the orders from the dynamic query */
	private JsonArray queryDynamic() throws Exception {
		return cli().postToJson( "/api/margin-dynamic", Util.toJson(
				"wallet_public_key", Cookie.wallet, 
				"cookie", Cookie.cookie,
				"conid", conid) )
			.getArray( "orders");
	}

}
