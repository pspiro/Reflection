package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import reflection.MarginOrder;
import reflection.MarginOrder.Status;
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
		// place order
		JsonObject orderJson = cli().postToJson( "/api/margin-order", newOrd() );
		assert200();

		// fail missing conid
		cli().postToJson( "/api/margin-dynamic", Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie
				) );
		failWith( RefCode.INVALID_REQUEST);
		assertStartsWith( "Param 'conid'", cli.getMessage() );

		// fail missing cookie
		cli().postToJson( "/api/margin-dynamic", Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"conid", conid
				) );
		failWith( RefCode.VALIDATION_FAILED);

		// fail missing wallet
		cli().postToJson( "/api/margin-dynamic", Util.toJson( 
				"cookie", Cookie.cookie,
				"conid", conid
				) );
		failWith( RefCode.INVALID_REQUEST);

		// fail wrong wallet
		cli().postToJson( "/api/margin-dynamic", Util.toJson( 
				"cookie", Cookie.cookie,
				"wallet_public_key", Cookie.dead,
				"conid", conid
				) );
		failWith( RefCode.VALIDATION_FAILED);

		// success
		JsonObject json = cli().postToJson( "/api/margin-dynamic", Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"conid", conid,
				"cookie", Cookie.cookie) );
		json.display();
		JsonObject order = json.getArray( "orders").find( "orderId", orderJson.getString( "orderId") );
		assertTrue( order != null);
		
		cancel( orderJson.getString( "orderId"));
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
	
	public void testBuyNoFill() throws Exception {
		S.out( "placing order");
		JsonObject json = cli().postToJson( "/api/margin-order", newOrd() );
		json.display();
		assert200();

		S.out( "wait to accept pmt and place buy order");
		waitFor(40, () -> getOrderStatus( json) == Status.PlacedBuyOrder);
		
		cancel( json.getString( "orderId"));
	}

	/** sell orders will be resting */
	public void testFillBuyOnly() throws Exception {
		JsonObject ord = newOrd();
		ord.put( "profitTakerPrice", base + 2);
		ord.put( "entryPrice", base + 1);
		ord.put( "stopLossPrice", base - 1);

		JsonObject json = cli().postToJson( "/api/margin-order", ord );
		json.display();
		assert200();

		S.out( "wait 5 sec to accept pmt and fill buy order");
		waitFor(40, () -> {
			Status status = getOrderStatus( json);
			return status == Status.BuyOrderFilled || status == Status.PlacedSellOrders;
		});
		
		// place another order; it should fail
		JsonObject json2 = cli().postToJson( "/api/margin-order", ord );
		json2.display();
		failWith( RefCode.INVALID_REQUEST, "There is already an open margin order");
		
		cancel( json.getString("orderId") );
	}

	/** sell orders will be resting */
	public void testFillBuyLev() throws Exception {
		JsonObject ord = newOrd();
		ord.put( "profitTakerPrice", base + 2);
		ord.put( "entryPrice", base + 1);
		ord.put( "stopLossPrice", base - 1);
		ord.put( "leverage", 3);

		JsonObject json = cli().postToJson( "/api/margin-order", ord );
		json.display();
		assert200();

		S.out( "wait 5 sec to accept pmt and fill buy order");
		waitFor(40, () -> {
			Status status = getOrderStatus( json);
			return status == Status.BuyOrderFilled || status == Status.PlacedSellOrders;
		});
		
		cancel( json.getString("orderId") );
	}

	public void testFillBuyAndStop() throws Exception {
		JsonObject ord = newOrd();
		ord.put( "profitTakerPrice", base + 3);
		ord.put( "entryPrice", base + 2);
		ord.put( "stopLossPrice", base + 1);
		ord.put( "test", true);  // this overrides the normal restriction that stop-loss price must be < market price
		
		S.out( "placing order");
		JsonObject json = cli().postToJson( "/api/margin-order", ord );
		assert200();
		
		waitFor(40, () -> getOrderStatus( json) == Status.Completed);
	}
	
	public void testUpdate() throws Exception {
		S.out( "placing order");
		JsonObject json = cli().postToJson( "/api/margin-order", newOrd() );
		assert200();
		
		String orderId = json.getString( "orderId");

		// fail no wallet
		cli().postToJson( "/api/margin-update", Util.toJson(
				"cookie", Cookie.cookie,
				"orderId", orderId,
				"profitTakerPrice", base + 1,
				"entryPrice", base - 1,
				"stopLossPrice", base - 2) );
		failWith( RefCode.INVALID_REQUEST, "Wallet address is missing");

		// fail wrong wallet
		cli().postToJson( "/api/margin-update", Util.toJson(
				"cookie", Cookie.cookie,
				"wallet_public_key", dead,
				"orderId", orderId,
				"profitTakerPrice", base + 1,
				"entryPrice", base - 1,
				"stopLossPrice", base - 2) );
		failWith( RefCode.VALIDATION_FAILED); // fails cookie validation, "Message wallet address");

		// fail no cookie
		cli().postToJson( "/api/margin-update", Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"orderId", orderId,
				"profitTakerPrice", base + 1,
				"entryPrice", base - 1,
				"stopLossPrice", base - 2) );
		failWith( RefCode.VALIDATION_FAILED);

		// fail unknown order id
		cli().postToJson( "/api/margin-update", Util.toJson(
				"cookie", Cookie.cookie,
				"wallet_public_key", Cookie.wallet,
				"orderId", "lkjsdflksj",
				"profitTakerPrice", base + 1,
				"entryPrice", base - 1,
				"stopLossPrice", base - 2) );
		failWith( RefCode.INVALID_REQUEST, "No such order found");

		// fail entry price has been increased; we could support this
		// but then we have to collect more $ from the user
		cli().postToJson( "/api/margin-update", Util.toJson(
				"cookie", Cookie.cookie,
				"wallet_public_key", Cookie.wallet,
				"orderId", orderId,
				"profitTakerPrice", base + 1,
				"entryPrice", base - .5,
				"stopLossPrice", base - 2) );
		failWith( RefCode.INVALID_PRICE, "The 'buy' price cannot be increased");
		
		// succeed
		cli().postToJson( "/api/margin-update", Util.toJson(
				"cookie", Cookie.cookie,
				"wallet_public_key", Cookie.wallet,
				"orderId", orderId,
				"profitTakerPrice", base + 1,
				"entryPrice", base - 1.1,
				"stopLossPrice", base - 2) );
		assert200();
		
		cancel( orderId);
	}
	
	public void testLiquidate1() throws Exception {
		S.out( "placing order with buy only");
		
		JsonObject ord = newOrd();
		ord.put( "entryPrice", base + .5);
		ord.remove( "profitTakerPrice");
		ord.remove( "stopLossPrice");
		
		JsonObject json = cli().postToJson( "/api/margin-order", ord);
		assert200();
		
		S.out( "wait to accept pmt and place buy order");
		waitFor(40, () -> getOrderStatus( json) == Status.Completed);
	}

	private MarginOrder.Status getOrderStatus(JsonObject json) throws Exception {
		JsonObject ret = cli().getToJson("/api/margin-get-status/" + json.getString( "orderId") );
		String status = ret.getString( "status");
		Util.require( S.isNotNull( status), "Error: no status for order " + json.getString( "orderId") );
		return Util.getEnum( status, MarginOrder.Status.values() );
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
		String orderId = json.getString( "orderId");

		// missing orderId
		S.out( "fail");
		cli().postToJson( "/api/margin-cancel",	Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie) )
			.display();
		failWith( RefCode.INVALID_REQUEST);
		assertStartsWith( "Param 'orderId'", cli.getMessage() );

		// fail wrong orderId
		S.out( "fail");
		cli().postToJson( "/api/margin-cancel",	Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"orderId", "xxxxxxxxxx",
				"cookie", Cookie.cookie) )
			.display();
		failWith( RefCode.INVALID_REQUEST);
		
		// fail wrong wallet
		S.out( "fail");
		cli().postToJson( "/api/margin-cancel",	Util.toJson( 
				"wallet_public_key", dead,
				"orderId", orderId,
				"cookie", Cookie.cookie) )
			.display();
		failWith( RefCode.VALIDATION_FAILED);
		
		// cancel, success
		S.out( "succeed");
		cancel( json.getString("orderId") );

		// fail already canceled
		S.out( "fail");
		cli().postToJson( "/api/margin-cancel",	Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"orderId", orderId,
				"cookie", Cookie.cookie) );
		failWith( RefCode.CANT_CANCEL);
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
