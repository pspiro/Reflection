package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import web3.NodeInstance;

public class TestMyWallet extends MyTestCase {
	static String empty = "0x3695889Ef1b0aC4F8d0479BCdb29fC5369C219ad";
	
	public void testBadWallet() throws Exception {
		assertEquals( 0., m_config.rusd().getPosition( empty));
	}
	
	public void testBadToken() throws Exception {
		try {
			node().getBalance( empty, Cookie.wallet, 22);
		}
		catch( Exception e) {
			return; // should come here
		}
		assertTrue(false);
	}
	
	public void testPosQuery() throws Exception {
		assertTrue( m_config.rusd().getPosition( Cookie.wallet) > 0);
		assertTrue( m_config.busd().getPosition( Cookie.wallet) > 0);
	}

	public void testMyWallet() throws Exception {
		Cookie.setWalletAddr(NodeInstance.prod);
		var param = Util.toJson( "cookie", Cookie.cookie);
				
		JsonObject obj = cli().postToJson("/api/mywallet/" + Cookie.wallet, param);
		assert200();
		
		obj.display("My Wallet");
		
		assertTrue( obj.getInt("refresh") > 100);
		
		JsonArray ar = obj.getArray("tokens");
		
		JsonObject tok;
		
		tok = ar.getJsonObj(1);
		assertEquals("BUSD", tok.getString("name"));
		assertTrue( tok.getDouble("balance") > 0 );
		assertTrue( tok.getDouble("approvedBalance") > 0 );

		tok = ar.getJsonObj(2);
		assertEquals(m_config.chain().params().platformBase(), tok.getString("name"));
		assertTrue( tok.getDouble("balance") > 0 );
	}
}
