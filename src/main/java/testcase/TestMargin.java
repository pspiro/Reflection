package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import reflection.RefCode;
import tw.util.S;

public class TestMargin extends MyTestCase {
	public void testStatic() throws Exception {
		cli().get("/api/margin-static/" + Cookie.wallet).readJsonObject().display();
		assert200();
	}

	public void testDynamic() throws Exception {
		cli().postToJson( "/api/margin-dynamic", Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie
				).toString() )
			.display();
		assertEquals( RefCode.INVALID_REQUEST, cli.getRefCode() );
		assertStartsWith( "Param 'conid'", cli.getMessage() );

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
		
		
		JsonObject live = cli().postToJson( "/api/margin-dynamic", Util.toJson(
				"wallet_public_key", Cookie.wallet, 
				"cookie", Cookie.cookie,
				"conid", "265598") );
		live.display();
		
		boolean found = false;
		
		for (var liveOrd : live.getArray( "orders") ) {
			if (liveOrd.getString("orderId").equals( json.getString("orderId"))) {
				found = true;
			}
		}
		assertTrue( found);
		
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
