package testcase;

import org.json.simple.JsonArray;

import common.Util;

public class TestGetCryptoTrans extends MyTestCase {
	public void testGetAll() throws Exception {
		JsonArray ar = cli().get( "/api/crypto-transactions")
			.readJsonArray();
		assertTrue( ar.size() > 0);
	}

	public void testGetSome() throws Exception {
		JsonArray ar = cli().post( "/api/crypto-transactions", Util.toJson("wallet_public_key", prodWallet).toString() )
				.readJsonArray();
		ar.display();
		assertTrue( ar.size() > 0);
	}
}
