package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import positions.Wallet;

public class TestWallet extends MyTestCase {
	static String empty = "0x3695889Ef1b0aC4F8d0479BCdb29fC5369C219ad";
	
	public void testBadWallet() throws Exception {
		Wallet wallet = new Wallet(empty);
		assertEquals( 0., wallet.getBalance( m_config.rusdAddr() ) );
	}
	
	public void testBadToken() throws Exception {
		Wallet wallet = new Wallet(Cookie.wallet);
		assertEquals( 0., wallet.getBalance( "badtoken") );
	}
	
	public void testPosQuery() throws Exception {
		Wallet wallet = new Wallet(Cookie.wallet);
		assertTrue( wallet.getBalance(m_config.rusdAddr() ) > 0);
		assertTrue( wallet.getBalance(m_config.busdAddr() ) > 0);
		
		assertEquals( wallet.getBalance(m_config.rusdAddr() ), m_config.rusd().getPosition(Cookie.wallet) );
		assertEquals( wallet.getBalance(m_config.busdAddr() ), m_config.busd().getPosition(Cookie.wallet) );
	}

	public void testMyWallet() throws Exception {
		JsonObject obj = cli().get("/api/mywallet/" + Cookie.wallet).readJsonObject();
		obj.display("My Wallet");
		
		assertTrue( obj.getInt("refresh") > 100);
		
		JsonArray ar = obj.getArray("tokens");
		
		JsonObject tok;
		
//		tok = ar.getJsonObj(0);  // moved to TestRedeem
//		assertEquals("RUSD", tok.getString("name"));
//		assertTrue( tok.getDouble("balance") > 0 );

		tok = ar.getJsonObj(1);
		assertEquals("USDC", tok.getString("name"));
		assertTrue( tok.getDouble("balance") > 0 );
		assertTrue( tok.getDouble("approvedBalance") > 0 );

		tok = ar.getJsonObj(2);
		assertEquals("MATIC", tok.getString("name"));
		assertTrue( tok.getDouble("balance") > 0 );
	}
	
	public void testWalletUpdate() throws Exception {
		JsonObject json = new JsonObject();
		json.put( "wallet_public_key", Cookie.wallet);
		json.put( "cookie", Cookie.cookie);

		cli().post("/api/wallet-update", json.toString() );
		assertTrue(false);
	}
}
