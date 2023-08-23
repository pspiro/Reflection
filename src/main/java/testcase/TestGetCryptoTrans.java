package testcase;

import org.json.simple.JsonArray;

import tw.util.S;

public class TestGetCryptoTrans extends MyTestCase {
	public void testGetAll() throws Exception {
		JsonArray ar = cli().get( "/api/crypto-transactions")
			.readJsonArray();
		assertTrue( ar.size() > 0);
	}

	public void testGetSome() throws Exception {
		JsonArray ar = cli().get( "/api/crypto-transactions/?wallet_public_key=" + Cookie.wallet)
				.readJsonArray();
		ar.display();
		assertTrue( ar.size() > 0);
	}
	
	public void testOpenTrans() throws Exception {
		m_config.sqlCommand( conn -> {
			S.out( "open connection");
			S.sleep(5000);
		});
	}
}
