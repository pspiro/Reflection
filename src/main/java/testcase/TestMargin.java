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
	
	static {
		try {
			if (m_config.rusd().getPosition(Cookie.wallet) < 1000) {
				m_config.rusd().mintRusd(Cookie.wallet, 100000, stocks.getAnyStockToken() )
					.displayHash();
			}
			
			S.out( "wallet_public_key=%s", Cookie.wallet);
			S.out( "cookie=%s", Cookie.cookie);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void testStaticQuery() throws Exception {
		S.out( "testing static");
		cli().get("/api/margin-static/" + Cookie.wallet).readJsonObject().display(); // cookie is not required but Frontend should pass it for debugging
		assert200();
	}

	public void testDynamicQuery() throws Exception {
		S.out( "testing dynamic");
		
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
		assert200();
		
		JsonObject order = json.getArray( "orders").find( "orderId", orderJson.getString( "orderId") );
		assertTrue( order != null);
		
		cancel( orderJson.getString( "orderId") );
	}

	public void testFailOrder() throws Exception {
		S.out( "testing fail order");
		
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
		S.out( "testing buy no fill");
		JsonObject json = cli().postToJson( "/api/margin-order", newOrd() );
		waitForStatus( json, Status.PlacedBuyOrder);
	}

	/** sell orders will be resting */
	public void testFillBuyNoSell() throws Exception {
		S.out( "testing fill buy no sell orders");
		JsonObject ord = newOrd();
		ord.put( "entryPrice", base + 1);
		ord.remove( "profitTakerPrice");
		ord.remove( "stopLossPrice");

		JsonObject json = cli().postToJson( "/api/margin-order", ord );
		waitForStatus( json, Status.Completed);
		
		// fail withdraw, please liquidate first
		S.out( "  fail withdrawal");
		JsonObject params = Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"orderId", json.getString( "orderId") );
		cli().postToJson("/api/margin-withdraw-funds", params);
		failWith( RefCode.CANT_WITHDRAW);
	}

	/** sell orders will be resting */
	public void testFillBuyOnly() throws Exception {
		S.out( "testing fill buy only");
		JsonObject ord = newOrd();
		ord.put( "profitTakerPrice", base + 2);
		ord.put( "entryPrice", base + 1);
		ord.put( "stopLossPrice", base - 1);

		JsonObject json = cli().postToJson( "/api/margin-order", ord );
		waitForStatus( json, Status.Completed);
	}

	/** start with no profit, fill buy, add profit, fill profit */
	public void testModProfitFromZero() throws Exception {
		S.out( "testing modify profit from zero");
		JsonObject ord = newOrd();
		ord.put( "leverage", 1.1);
		ord.put( "entryPrice", base + .5);
		ord.remove( "profitTakerPrice");
		ord.remove( "stopLossPrice");

		JsonObject json = cli().postToJson( "/api/margin-order", ord );
		waitForStatus( json, Status.Monitoring, false);

		// modify profit price to fill
		ord.remove( "entryPrice");
		ord.put( "orderId", json.getString( "orderId") );
		ord.put( "profitTakerPrice", base -1);
		ord.put( "stopLossPrice", base - 2);
		ord.put( "test", true);
		
		cli().postToJson( "/api/margin-update", ord );
		waitForStatus( json, Status.Completed);
	}

	/** start w/ no profit, add a profit before filling buy, then fill buy */
	public void testModProfitEarlyFromZero() throws Exception {
		S.out( "testing modify profit early from zero");
		JsonObject ord = newOrd();
		ord.remove( "profitTakerPrice");
		ord.remove( "stopLossPrice");

		JsonObject json = cli().postToJson( "/api/margin-order", ord );
		waitForStatus( json, Status.PlacedBuyOrder, false);

		// add a profit price
		ord.remove( "entryPrice");
		ord.put( "orderId", json.getString( "orderId") );
		ord.put( "profitTakerPrice", base + 1);
		cli().postToJson( "/api/margin-update", ord );
		assert200();
		
		// modify entry price to fill
		ord.put( "entryPrice", base + .25);
		ord.put( "orderId", json.getString( "orderId") );
		ord.remove( "profitTakerPrice");
		cli().postToJson( "/api/margin-update", ord );
		waitForStatus( json, Status.Monitoring, true);
	}

	/** start w/ no stop, fill buy, then add a stop, let it fill */
	public void testModStopFromZero() throws Exception {
		S.out( "testing modify stop from zero");
		JsonObject ord = newOrd();
		ord.put( "leverage", 1.1);
		ord.put( "entryPrice", base + .5);
		ord.remove( "profitTakerPrice");
		ord.remove( "stopLossPrice");

		JsonObject json = cli().postToJson( "/api/margin-order", ord );
		waitForStatus( json, Status.Monitoring, false);

		// modify stop price to fill
		ord.remove( "entryPrice");
		ord.put( "orderId", json.getString( "orderId") );
		ord.put( "profitTakerPrice", base + 2);
		ord.put( "stopLossPrice", base + 1);
		ord.put( "test", true);
		
		cli().postToJson( "/api/margin-update", ord );
		waitForStatus( json, Status.Completed);
	}


	/** leverage order should be monitored for liquidation */
	public void testFillBuyLev() throws Exception {
		S.out( "testing fill buy lev");
		JsonObject ord = newOrd();
		ord.put( "entryPrice", base + 1);
		ord.put( "leverage", 3);
		ord.remove( "profitTakerPrice");
		ord.remove( "stopLossPrice");

		JsonObject json = cli().postToJson( "/api/margin-order", ord );
		waitForStatus( json, Status.Monitoring);
	}

	public void testFillStop() throws Exception {
		S.out( "testing fill buy and stop");
		
		JsonObject ord = newOrd();
		ord.put( "profitTakerPrice", base + 3);
		ord.put( "entryPrice", base + 2);
		ord.put( "stopLossPrice", base + 1);
		ord.put( "test", true);  // this overrides the normal restriction that stop-loss price must be < market price
		
		S.out( "placing order");
		JsonObject json = cli().postToJson( "/api/margin-order", ord );
		waitForStatus( json, Status.Completed);
	}
	
	public void testFillProfitAndWithdraw() throws Exception {
		S.out( "testing fill buy and profit requires price update");

		// place order
		JsonObject ord = newOrd();
		ord.put( "profitTakerPrice", base + 3);
		ord.put( "entryPrice", base + 2);
		ord.put( "stopLossPrice", base - 4);
		
		// wait for buy order to fill
		S.out( "  waiting for buy order to fill");
		JsonObject json = cli().postToJson( "/api/margin-order", ord ); // it's skipping over this and going right to completed; how is that?
		waitForStatus(json, Status.Monitoring, false);

		// fail withdraw, wrong order status
		S.out( "  fail withdrawal");
		JsonObject params = Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"orderId", json.getString( "orderId") );
		cli().postToJson("/api/margin-withdraw-funds", params);
		failWith( RefCode.CANT_WITHDRAW);

		// update order with low sell price, wait for sell to fill
		ord.put( "orderId", json.getString( "orderId") );
		ord.put( "profitTakerPrice", base - 1);
		cli().postToJson( "/api/margin-update", ord );
		waitForStatus( json, Status.Completed);

		// withdraw successfully
		cli().postToJson("/api/margin-withdraw-funds", params);
		assert200();
	}
	
	// the problem is the order hasn't filled or even been placed yet, you can't modify it!!!
	// check the state
	public void testUpdate() throws Exception {
		S.out( "testing update");
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
	
	/** user-initiated liquidation */
	public void testUserLiq() throws Exception {
		S.out( "testing user liquidation");
		
		JsonObject ord = newOrd();
		ord.put( "entryPrice", base + .5);
		ord.put( "leverage", 2);
		
		JsonObject json = cli().postToJson( "/api/margin-order", ord);
		waitForStatus( json, Status.Monitoring, false);
		
		JsonObject param = Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"orderId", json.getString( "orderId") );
		cli().postToJson( "/api/margin-liquidate", param).display();
		waitForStatus( json, Status.Completed);
	}


	/** sim tomorrow's close; before running this, check the trading hours:
	 *  /api/?msg=getTradingHours
	 *  and make sure the next closed day is showing up there  */
	public void testLiquidate1() throws Exception {
		S.out( "testing liquidate1");
		
		JsonObject ord = newOrd();
		ord.put( "entryPrice", base + .5);
		ord.put( "leverage", 2);
		
		JsonObject json = cli().postToJson( "/api/margin-order", ord);
		waitForStatus( json, Status.Monitoring, false);

		S.out( "simulating day before close");
		long time = System.currentTimeMillis() + Util.DAY * 2;  // set this to be the day before the market is closed
		cli().get( "/api/?msg=simulate&item=time&time=" + time);
		waitForStatus( json, Status.Completed);
	}

	/** sim price drop*/
	public void testLiqPriceDrop() throws Exception {
		S.out( "testing liquidate1");
		
		JsonObject ord = newOrd();
		ord.put( "entryPrice", base + .5);
		ord.put( "leverage", 10);
		
		JsonObject json = cli().postToJson( "/api/margin-order", ord);
		waitForStatus( json, Status.Monitoring, false);
		
		S.out( "got to Monitoring phase; simulating price drop");
		cli().get( String.format( "/api/?msg=simulate&item=price&conid=%s&price=%s", TestOrder.conid, base * .85) );
		assert200();
		
		waitForStatus( json, Status.Completed);
		S.out( "status should go to completed for %s", json.getString( "orderId"));
		
		assertTrue( false);
	}

	public void testAddFunds() {
		assertTrue( false);
	}

	/** fails with Please liquidate your position before withdrawing the cash */
	public void testWithdrawFunds() throws Exception {
		JsonObject json = cli().postToJson( "/api/margin-order", newOrd().modify( 
				"profitTakerPrice", 0,
				"entryPrice", base + 1,
				"stopLossPrice", 0
				)
		);
		waitForStatus( json,  Status.Completed);
		
		double recBal = stocks.getReceipt().getPosition( Cookie.wallet);
		
		JsonObject params = Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"orderId", json.getString( "orderId") );
		cli().postToJson("/api/margin-withdraw-funds", params);
		assert200();
		
		// second time should fail 
		cli().postToJson("/api/margin-withdraw", params);
		failWith( RefCode.INVALID_REQUEST, "Funds cannot be withdrawn");

		waitForStatus( json, Status.Settled);

		waitForBalance( Cookie.wallet, stocks.getReceipt().address(), recBal - json.getDouble( "amountToSpend"), true);
	}

	public void testWithdrawTokens() {
		assertTrue( false);
	}


	public void testCancel() throws Exception {
		S.out( "testing cancel");
		JsonObject json = cli().postToJson( "/api/margin-order", newOrd() );
		assert200();
		json.display();

		String orderId = json.getString( "orderId");
		assertEquals( 10, orderId.length() );

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
				"cookie", Cookie.cookie,
				"system", true  // system cancel can force a cancel which would otherwise fail
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
	
	private MarginOrder.Status getOrderStatus(JsonObject json) throws Exception {
		JsonObject ret = cli().getToJson("/api/margin-get-status/" + json.getString( "orderId") );
		String status = ret.getString( "status");
		Util.require( S.isNotNull( status), "Error: no status for order " + json.getString( "orderId") );
		return Util.getEnum( status, MarginOrder.Status.values() );
	}
	
	/** Wait for status and then cancel the order, pass or fail */
	void waitForStatus(JsonObject json, Status status) throws Exception {
		waitForStatus( json, status, true);
	}
	
	void waitForStatus(JsonObject json, Status status, boolean cancel) throws Exception {
		json.display();
		assert200();

		try {
			S.out( "waiting for status '%s'", status);
			waitFor(5000, () -> getOrderStatus( json) == status);
		}
		finally {
			if (cancel && status != Status.Completed) {
				cancel( json.getString( "orderId"));
			}
		}
	}
	
	/** very close, but pnl is off */
	public void testSummary() throws Exception {
		S.out( "testing summary");
		
		JsonObject ord = newOrd();
		ord.put( "entryPrice", base + 2);
		ord.put( "profitTakerPrice", base + 3);
		ord.put( "stopLossPrice", base + 1);
		ord.put( "test", true);  // this overrides the normal restriction that stop-loss price must be < market price
		
		S.out( "placing order");
		JsonObject json = cli().postToJson( "/api/margin-order", ord );
		waitForStatus( json, Status.Monitoring, false);
		
		JsonObject sum = cli().postToJson( "/api/margin-summary", Util.toJson( 
						"wallet_public_key", Cookie.wallet,
						"cookie", Cookie.cookie,
						"orderId", json.getString( "orderId")
						) );
		assert200();
		
		sum.display();
		
		sum.getArray( "top").print();
		sum.getArray( "bottom").print();


	}

}
