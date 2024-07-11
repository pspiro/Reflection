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
	
	public void testStatic() throws Exception {
		cli().get("/api/margin-static/" + Cookie.wallet).readJsonObject().display(); // cookie is not required but Frontend should pass it for debugging
		assert200();
	}

	public void testDynamic() throws Exception {
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
				"conid", "265598"
				) )
			.display();
		failWith( RefCode.VALIDATION_FAILED);

		// success
		cli().postToJson( "/api/margin-dynamic", Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"conid", "265598",
				"cookie", Cookie.cookie
				) )
			.display();
		assert200();
	}

	public void testFailUserProfile() throws Exception {
		Cookie.setNewFakeAddress( false);
		cli().postToJson( "/api/margin-order", newOrd() );
		failWith( RefCode.INVALID_USER_PROFILE);

		Cookie.setNewFakeAddress( true);
		cli().postToJson( "/api/margin-order", newOrd() );
		assert200();
	}

	public void testBuyNoFill() throws Exception {
		S.out( "placing order");
		JsonObject json = cli().postToJson( "/api/margin-order", newOrd() );
		json.display();
		assert200();

		// test retrieving the order - fail invalid order id
		cli().getToJson("/api/margin-get-order/" + json.getString( "abc") ).display();
		assert400();
		
		// test retrieving the order - fail no such order
		cli().getToJson("/api/margin-get-order/" + json.getString( "abcdeghij") ).display();
		assert400();

		// retrieve the order
		cli().getToJson("/api/margin-get-order/" + json.getString( "orderId") ).display();
		assert200();

		S.out( "wait to accept pmt and place buy order");
		int i = Util.waitFor(20, () -> {
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
		int i = Util.waitFor(20, () -> {
			String status = queryDynamic().find( "orderId", json.getString("orderId") )
					.getString( "status");
			return status.equals( "BuyOrderFilled") || status.equals( "PlacedSellOrders");
		});
		
//		cancel( json.getString("orderId") );
		
		assertTrue( i >= 0);
	}

	public void testFillBuyAndStop() throws Exception {
		JsonObject ord = Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"conid", "265598",
				"amountToSpend", 100.12,
				"leverage", 1.,
				"profitTakerPrice", 226,
				"entryPrice", 225,
				"stopLossPrice", 224,
				"goodUntil", "EndOfDay",
				"currency", "RUSD"
				);
		S.out( "placing order");
		JsonObject json = cli().postToJson( "/api/margin-order", ord );
		assert200();
		
		S.out( "wait 5 sec to fill");
		S.sleep( 5000);
		
		JsonObject live = queryDynamic().find( "orderId", json.getString("orderId") );
		assertEquals( Status.Completed, live.getString( "status") );
	}

	public void testFullOrder() throws Exception {
		JsonObject ord = Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"conid", "265598",
				"amountToSpend", 100.12,
				"leverage", 1.,
				"profitTakerPrice", 216,
				"entryPrice", 215,
				"stopLossPrice", 214.99,
				"goodUntil", "EndOfDay",
				"currency", "RUSD"
				);
		S.out( "placing order");
		JsonObject json = cli().postToJson( "/api/margin-order", ord );
		assert200();
		assertEquals( 10, json.getString( "orderId").length() );
		json.display();
		
		
		JsonObject dynamic = cli().postToJson( "/api/margin-dynamic", Util.toJson(
				"wallet_public_key", Cookie.wallet, 
				"cookie", Cookie.cookie,
				"conid", "265598") );
		
		JsonObject live = dynamic.getArray( "orders").find( "orderId", json.getString("orderId") );
		assertTrue( "live order not found", live != null);
		live.display();
		
		// add assertions here
		
		
		// cancel, missing orderId
		cli().postToJson( "/api/margin-cancel",	Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie
				) )
			.display();
		assertEquals( RefCode.INVALID_REQUEST, cli.getRefCode() );
		assertStartsWith( "Param 'orderId'", cli.getMessage() );

		// cancel, wrong orderId
		cli().postToJson( "/api/margin-cancel",	Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"orderId", "myorderid",
				"cookie", Cookie.cookie
				) )
			.display();
		assertEquals( RefCode.INVALID_REQUEST, cli.getRefCode() );
		
		// cancel, success
		cancel( json.getString("orderId") );
	}
	
	// need to test cancel from more states, pass and fail
	public void testCancel() throws Exception {
		JsonObject ord = Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"conid", "265598",
				"amountToSpend", 100.12,
				"leverage", 1.,
				"profitTakerPrice", 216,
				"entryPrice", 215,
				"stopLossPrice", 214.99,
				"goodUntil", "EndOfDay",
				"currency", "RUSD"
				);
		JsonObject json = cli().postToJson( "/api/margin-order", ord );
		assert200();
		String id = json.getString( "orderId");
		
		for (int i = 0; i < 3; i++) {
			showStatus( id);
			S.sleep( 500);
		}
		
		// cancel, success
		S.out( "canceling");
		cli().postToJson( "/api/margin-cancel",	Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"orderId", id,
				"cookie", Cookie.cookie) );
		assert200();
		
		for (int i = 0; i < 15; i++) {
			showStatus( id);
			S.sleep( 1000);
		}
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
	
	public void testmarginLiquidate() {
	}

	public void testmarginAddFunds() {
	}

	public void testmarginWithdrawFunds() {
	}

	public void testmarginWithdrawTokens() {
	}

	static JsonObject newOrd() {
		return Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"conid", "265598",
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
				"conid", "265598",
				"cookie", Cookie.cookie
				) ).getArray( "orders");

		Util.iff( ords.find( "orderId", id), ord -> S.out( ord) );
	}
	
	private JsonArray queryDynamic() throws Exception {
		return cli().postToJson( "/api/margin-dynamic", Util.toJson(
				"wallet_public_key", Cookie.wallet, 
				"cookie", Cookie.cookie,
				"conid", "265598") )
			.getArray( "orders");
	}

}
