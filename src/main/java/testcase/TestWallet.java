package testcase;

import http.MyHttpClient;
import json.MyJsonObject;
import positions.Wallet;

public class TestWallet extends MyTestCase {
	static String empty = "0x3695889Ef1b0aC4F8d0479BCdb29fC5369C219ad";
	
	public void testBadWallet() throws Exception {
		Wallet wallet = new Wallet(empty);
		assertEquals( 0., wallet.getBalance( config.rusdAddr() ) );
	}
	
	public void testBadToken() throws Exception {
		Wallet wallet = new Wallet(Cookie.wallet);
		assertEquals( 0., wallet.getBalance( "badtoken") );
	}
	
	public void testPosQuery() throws Exception {
		Wallet wallet = new Wallet(Cookie.wallet);
		assertTrue( wallet.getBalance(config.rusdAddr() ) > 0);
		assertTrue( wallet.getBalance(config.busdAddr() ) > 0);
		assertTrue( wallet.getBalance(config.busdAddr() ) > 0);
		
		assertEquals( wallet.getBalance(config.rusdAddr() ), config.rusd().getPosition(Cookie.wallet) );
		assertEquals( wallet.getBalance(config.busdAddr() ), config.busd().getPosition(Cookie.wallet) );
	}

	public void testMyWallet() throws Exception {
		MyHttpClient cli = cli();
		MyJsonObject obj = cli.get("/api/mywallet/" + Cookie.wallet).readMyJsonObject();
		obj.display("My Wallet");
		
		assertTrue( obj.getInt("refresh") >= 100);

		MyJsonObject tok = obj.getObj("nativeToken");
		assertEquals("MATIC", tok.getString("name"));
		assertTrue( tok.getDouble("balance") > 0 );
		
		tok = obj.getObj("rusd");
		assertEquals("RUSD", tok.getString("name"));
		assertTrue( tok.getDouble("balance") > 0 );
		
		tok = obj.getObj("stablecoin");
		assertEquals("USDC", tok.getString("name"));
		assertTrue( tok.getDouble("balance") > 0 );
		assertTrue( tok.getDouble("approvedBalance") > 0 );
		
	}
}
