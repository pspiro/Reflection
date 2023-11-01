package testcase;

import org.json.simple.JsonObject;

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
		assertEquals( RefCode.NEED_KYC, cli.getRefCode() );
		
		m_config.sqlCommand( sql -> sql.execWithParams(
				"update users set kyc_status = true where wallet_public_key = '%s'",
				Cookie.wallet) );
		
		qty = m_config.nonKycMaxOrderSize() / price - 10;
		obj = TestOrder.createOrder2("buy", qty, price);
		postOrderToObj(obj);
		assert200();
	}

}
