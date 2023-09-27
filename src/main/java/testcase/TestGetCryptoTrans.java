package testcase;

import org.json.simple.JsonArray;

import common.Util;
import tw.util.S;

public class TestGetCryptoTrans extends MyTestCase {
	public void testGetAll() throws Exception {
		JsonArray ar = cli().get( "/api/crypto-transactions")
			.readJsonArray();
		assertTrue( ar.size() > 0);
	}

	public void testGetSome() throws Exception {
		JsonArray ar = cli().post( "/api/crypto-transactions", Util.toJson("wallet_public_key", "0xab015eb8298e5364cea5c8f8e084e2fc3e3bdead").toString() )
				.readJsonArray();
		ar.display();
		assertEquals(0, ar.size() );
	}
}
