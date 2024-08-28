package testcase;

import org.json.simple.JsonObject;

import common.Util;
import reflection.RefCode;
import tw.util.S;

public class TestKyc extends MyTestCase {

	public void testKyc()  throws Exception {
		// this must come first to trigger static code from TestOrder
		double price = TestOrder.curPrice * 1.1;

		// clear kyc status in database
		m_config.sqlCommand( sql -> sql.execWithParams(
				"update users set kyc_status = '', persona_response = '' where wallet_public_key = '%s'",
				Cookie.wallet.toLowerCase() ) );

		// confirm database was updated
		S.sleep(500);
		JsonObject row = m_config
				.sqlQuery(String.format("select * from users where wallet_public_key = '%s'", Cookie.wallet.toLowerCase() ) )
				.get(0);
		row.display();
		assertEquals("", S.notNull(row.getString("kyc_status")));
		
		// place an order larger than kyc; it should fail
		double qty = m_config.nonKycMaxOrderSize() / price + 10;
		mintRusd(Cookie.wallet, qty * price + 1); // give user sufficient stablecoin
		JsonObject order = TestOrder.createOrderWithPrice("buy", qty, price);
		postOrderToObj(order);
		assertEquals( RefCode.NEED_KYC, cli.getRefCode() );
		
		// update the kyc info with failed status and check/fail
		JsonObject data = Util.toJson(
				"wallet_public_key", Cookie.wallet.toLowerCase(),
				"cookie", Cookie.cookie,
				"kyc_status", "blahblah",  // ignored
				"persona_response", "{\"phone\":\"9143933732\", \"status\": \"failed\" }",
				"country", "my country",
				"city", "my city");		
		cli().post("/api/users/register", data.toString() );
		//failWith( RefCode.INVALID_REQUEST, "KYC failed");
		assertEquals( 400, cli.getResponseCode() );  // returns 400 even though the database is updated
		JsonObject retJson = cli().postToJson( "/api/check-identity", data.toString() );
		assertFalse( retJson.getBool( "verified") );

		// update the kyc info with completed status and check/succeed
		data.put( "persona_response", "{\"phone\":\"9143933732\", \"status\": \"completed\" }");
		retJson = cli().postToJson( "/api/users/register", data.toString() );
		assertEquals( 200, cli.getResponseCode() ); // don't use assert200 because refcode OK is not returned
		retJson = cli().postToJson( "/api/check-identity", data.toString() );  // check it - succeed
		assertTrue( retJson.getBool( "verified") );
		
		// confirm it still works with VERIFIED
		m_config.sqlCommand( sql -> sql.execWithParams("update users set kyc_status='VERIFIED' where wallet_public_key = '%s'",
				Cookie.wallet) );
		retJson = cli().postToJson( "/api/check-identity", data.toString() );
		assertTrue( retJson.getBool( "verified") );

		S.sleep(500);
		
		// confirm database was updated
		S.sleep(500);
		row = m_config
				.sqlQuery(String.format("select * from users where wallet_public_key = '%s'", Cookie.wallet.toLowerCase() ) )
				.get(0);
		row.display();
		assertEquals("completed", row.getString("kyc_status"));

		// place another order; it should pass
		qty = m_config.nonKycMaxOrderSize() / price + 10;
		order = TestOrder.createOrderWithPrice("buy", qty, price);
		postOrderToObj(order);
		assert200();
	}
		

}
