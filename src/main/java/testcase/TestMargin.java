package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import reflection.MarginOrder.Status;
import reflection.RefCode;
import tw.util.S;

public class TestMargin extends MyTestCase {
	public void testStatic() throws Exception {
		cli().get("/api/margin-static/" + Cookie.wallet).readJsonObject().display();
		assert200();
	}
	
	public void testShowBal() throws Exception {
		//S.out( "RUSD balance = %s", m_config.rusd().getPosition(Cookie.wallet) );
		Cookie.setNewFakeAddress( false);

		JsonObject ord = Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"conid", "265598",
				"amountToSpend", 100.12,
				"leverage", 1.,
				"profitTakerPrice", 226,
				"entryPrice", 225,
				"stopLossPrice", 200,
				"goodUntil", "EndOfDay",
				"currency", "RUSD"
				);
		cli().postToJson( "/api/margin-order", ord.toString() );
		assertEquals( RefCode.INVALID_USER_PROFILE, cli.getRefCode() ); // fail due to no profile

		Cookie.setNewFakeAddress( true);
		ord = Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"conid", "265598",
				"amountToSpend", 100.12,
				"leverage", 1.,
				"profitTakerPrice", 226,
				"entryPrice", 225,
				"stopLossPrice", 200,
				"goodUntil", "EndOfDay",
				"currency", "RUSD"
				);
		cli().postToJson( "/api/margin-order", ord.toString() );
		assert200();
		//assertEquals( RefCode.INVALID_REQUEST, cli.getRefCode() ); // fail due to insufficient stablecoin
	}
	
	public void testDynamic() throws Exception {
		// fail, missing conid
		cli().postToJson( "/api/margin-dynamic", Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie
				).toString() )
			.display();
		assertEquals( RefCode.INVALID_REQUEST, cli.getRefCode() );
		assertStartsWith( "Param 'conid'", cli.getMessage() );

		// fail, missing cookie
		cli().postToJson( "/api/margin-dynamic", Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"conid", "265598"
				).toString() )
			.display();
		assertEquals( RefCode.VALIDATION_FAILED, cli.getRefCode() );

		// success
		cli().postToJson( "/api/margin-dynamic", Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"conid", "265598",
				"cookie", Cookie.cookie
				).toString() )
			.display();
		assert200();
	}
	
	public void testOrderStats() throws Exception {
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
		JsonObject json = cli().postToJson( "/api/margin-order", ord.toString() );
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

	private void showStatus(String id) throws Exception {
		JsonArray ords = cli().postToJson( "/api/margin-dynamic", Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"conid", "265598",
				"cookie", Cookie.cookie
				) ).getArray( "orders");

		Util.iff( ords.find( "orderId", id), ord -> 
			//S.out( "status: %s", ord.getString( "status") ) );
			S.out( ord) );
	}
	
	private JsonArray queryDynamic() throws Exception {
		return cli().postToJson( "/api/margin-dynamic", Util.toJson(
				"wallet_public_key", Cookie.wallet, 
				"cookie", Cookie.cookie,
				"conid", "265598") )
			.getArray( "orders");
	}

	public void testBuyNoFill() throws Exception {
		JsonObject ord = Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"conid", "265598",
				"amountToSpend", 100.12,
				"leverage", 1.,
				"profitTakerPrice", 226,
				"entryPrice", 220,
				"stopLossPrice", 200,
				"goodUntil", "EndOfDay",
				"currency", "RUSD"
				);
		S.out( "placing order");
		JsonObject json = cli().postToJson( "/api/margin-order", ord.toString() );
		assert200();

		S.out( "wait to accept pmt and fill buy order");
		int i = Util.waitFor(20, () -> {
			JsonObject live = queryDynamic().find( "orderId", json.getString("orderId") );
			return live.getString( "status") == "BuyOrderPlaced";
		});
		
		assertTrue( i >= 0);
	}

	public void testFillBuyOnly() throws Exception {
		JsonObject ord = Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"conid", "265598",
				"amountToSpend", 100.12,
				"leverage", 1.,
				"profitTakerPrice", 226,
				"entryPrice", 225,
				"stopLossPrice", 200,
				"goodUntil", "EndOfDay",
				"currency", "RUSD"
				);
		S.out( "placing order");
		JsonObject json = cli().postToJson( "/api/margin-order", ord.toString() );
		assert200();

		S.out( "wait 5 sec to accept pmt and fill buy order");
		int i = Util.waitFor(20, () -> {
			JsonObject live = queryDynamic().find( "orderId", json.getString("orderId") );
			return live.getString( "status") == "BuyOrderFilled" || live.getString( "status") == "PlacedSellOrders";
		});
		
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
		JsonObject json = cli().postToJson( "/api/margin-order", ord.toString() );
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
		JsonObject json = cli().postToJson( "/api/margin-order", ord.toString() );
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
				).toString() )
			.display();
		assertEquals( RefCode.INVALID_REQUEST, cli.getRefCode() );
		assertStartsWith( "Param 'orderId'", cli.getMessage() );

		// cancel, wrong orderId
		cli().postToJson( "/api/margin-cancel",	Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"orderId", "myorderid",
				"cookie", Cookie.cookie
				).toString() )
			.display();
		assertEquals( RefCode.INVALID_REQUEST, cli.getRefCode() );

		// cancel, success
		cli().postToJson( "/api/margin-cancel",	Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"orderId", json.getString("orderId"),
				"cookie", Cookie.cookie
				).toString() )
			.display();
		assert200();

	}
}
