package testcase;

import org.json.simple.JsonObject;

import common.Util;
import reflection.RefCode;
import tw.util.S;

public class TestKyc extends MyTestCase {
	public void testKyc()  throws Exception {
		m_config.sqlCommand( sql -> sql.execWithParams(
				"update users set kyc_status = false where wallet_public_key = '%s'",
				Cookie.wallet) );
		
		double price = TestOrder.curPrice * 1.1;
		
		double qty = m_config.nonKycMaxOrderSize() / price + 10;
		double amt = qty * price;
		S.out( "wal=%s  qty = %s  amt=%s", Cookie.wallet, qty, amt);
		JsonObject obj = TestOrder.createOrder2("buy", qty, price);
		postOrderToObj(obj);
		S.out(cli.readJsonObject().get("message"));
		assertEquals( RefCode.NEED_KYC, cli.getRefCode() );
		
		m_config.sqlCommand( sql -> sql.execWithParams(
				"update users set kyc_status = true where wallet_public_key = '%s'",
				Cookie.wallet) );
		
		qty = m_config.nonKycMaxOrderSize() / price - 10;
		obj = TestOrder.createOrder2("buy", qty, price);
		postOrderToObj(obj);
		assert200();
	}
	
	public void testSetKyc() throws Exception {
		m_config.sqlCommand( sql -> sql.delete( "delete from users where wallet_public_key = '%s'",  Cookie.wallet.toLowerCase() ) );

		assertEquals(0, m_config
				.sqlQuery(String.format("select * from users where wallet_public_key = '%s'", Cookie.wallet.toLowerCase() ) )
				.size() );
		
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
