package testcase;

import org.json.simple.JsonObject;

import common.Util;
import reflection.RefCode;

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
				"conid", "8314",
				"cookie", Cookie.cookie
				).toString() )
			.display();
		assert200();
	}

	public void testOrder() throws Exception {
		JsonObject ord = Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"conid", "265598",
				"amountToSpend", 100.12,
				"leverage", 1.5,
				"profitTakerPrice", 83.3,
				"entryPrice", 83.2,
				"stopLossPrice", 83.1,
				"goodUntil", "EndOfDay",
				"currency", "RUSD"
				);
		JsonObject json = cli().postToJson( "/api/margin-order", ord.toString() );
		json.display();
		assert200();
	}

	public void testCancel() throws Exception {
		cli().postToJson( "/api/margin-cancel",	Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie
				).toString() )
			.display();
		assertEquals( RefCode.INVALID_REQUEST, cli.getRefCode() );
		assertStartsWith( "Param 'orderId'", cli.getMessage() );

		cli().postToJson( "/api/margin-cancel",	Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"orderId", "myorderid",
				"cookie", Cookie.cookie
				).toString() )
			.display();
		assert200();
	}
}
