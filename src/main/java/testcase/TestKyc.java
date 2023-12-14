package testcase;

import org.json.simple.JsonObject;

import common.Util;
import reflection.RefCode;
import tw.util.S;

public class TestKyc extends MyTestCase {
	public void testKyc()  throws Exception {
		// clear kyc status
		m_config.sqlCommand( sql -> sql.execWithParams(
				"update users set kyc_status = '', persona_response = false where wallet_public_key = '%s'",
				Cookie.wallet) );

		// place an order larger than kyc; it should fail
		double price = TestOrder.curPrice * 1.1;
		double qty = m_config.nonKycMaxOrderSize() / price + 10;
		JsonObject order = TestOrder.createOrder2("buy", qty, price);
		postOrderToObj(order);
		S.out(cli.readJsonObject().get("message"));
		assertEquals( RefCode.NEED_KYC, cli.getRefCode() );

		// set key status to true
		m_config.sqlCommand( sql -> sql.execWithParams(
				"update users set kyc_status = true where wallet_public_key = '%s'",
				Cookie.wallet) );

		// place another order; it should pass
		qty = m_config.nonKycMaxOrderSize() / price + 10;
		order = TestOrder.createOrder2("buy", qty, price);
		postOrderToObj(order);
		assert200();
	}
	
	public void testSetKyc() throws Exception {
		// clear
		JsonObject d1 = Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"kyc_status", "false",
				"persona_response", "empty");
		m_config.sqlCommand( sql -> sql.updateJson( "users", d1, "wallet_public_key = '%s'", Cookie.wallet) ); 

		JsonObject data = Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"kyc_status", "OKKK",
				"persona_response", "my persona response",
				"country", "my country",
				"city", "my city");
		data.put("cookie", Cookie.cookie);
		
		cli().post("/api/users/register", data.toString() );
		assert200();
		
		S.sleep(1000);
		JsonObject row = m_config
				.sqlQuery(String.format("select * from users where wallet_public_key = '%s'", Cookie.wallet.toLowerCase() ) )
				.get(0);
		assertEquals("OKKK", row.getString("kyc_status"));
	}
		

}
