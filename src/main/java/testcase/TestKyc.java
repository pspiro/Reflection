package testcase;

import org.json.simple.JsonObject;

import common.Util;
import reflection.RefCode;
import tw.util.S;

public class TestKyc extends MyTestCase {

	public void testKyc()  throws Exception {
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
		double price = TestOrder.curPrice * 1.1;
		double qty = m_config.nonKycMaxOrderSize() / price + 10;
		JsonObject order = TestOrder.createOrderWithPrice("buy", qty, price);
		postOrderToObj(order);
		assertEquals( RefCode.NEED_KYC, cli.getRefCode() );
		
		// update the kyc info
		JsonObject data = Util.toJson(
				"wallet_public_key", Cookie.wallet.toLowerCase(),
				"kyc_status", "VERIFIED",
				"persona_response", "{\"phone\":\"9143933732\" }",
				"country", "my country",
				"city", "my city");		
		cli().post("/api/users/register", data.toString() );

		// fail w/ no cookie
		assertEquals( RefCode.VALIDATION_FAILED, cli.getRefCode() );
		
		// update it w/ cookie; success
		data.put("cookie", Cookie.cookie);
		cli().post("/api/users/register", data.toString() );
		assert200();
		
		S.sleep(500);
		
		// confirm database was updated
		S.sleep(500);
		row = m_config
				.sqlQuery(String.format("select * from users where wallet_public_key = '%s'", Cookie.wallet.toLowerCase() ) )
				.get(0);
		row.display();
		assertEquals("VERIFIED", row.getString("kyc_status"));

		// place another order; it should pass
		qty = m_config.nonKycMaxOrderSize() / price + 10;
		order = TestOrder.createOrderWithPrice("buy", qty, price);
		postOrderToObj(order);
		assert200();
	}
		

}
