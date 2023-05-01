package testcase;

import json.MyJsonArray;
import tw.util.S;

public class TestGetCryptoTrans extends MyTestCase {
	public void testGetAll() throws Exception {
		MyJsonArray ar = cli().get( "/api/crypto-transactions")
			.readMyJsonArray();
		assertTrue( ar.size() > 0);
	}

	public void testGetSome() throws Exception {
		MyJsonArray ar = cli().get( "/api/crypto-transactions/?wallet_public_key=" + Cookie.wallet)
				.readMyJsonArray();
		ar.display();
		assertTrue( ar.size() > 0);
	}
	
	public void testOpenTrans() throws Exception {
		config.sqlConnection( conn -> {
			S.out( "open connection");
			S.sleep(5000);
		});
	}
}
