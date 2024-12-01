package testcase;

import org.json.simple.JsonObject;

import common.Util;
import tw.util.S;

public class TestSignup extends MyTestCase {
//	public void testCheckForSignups() throws Exception {
//		String email = "email" + Util.rnd.nextInt();  // must be a valid email to work
//		
//		JsonObject obj = Util.toJson( 
//				"first", "first",
//				"last", "last",
//				"email", email);
//		cli().post("/api/signup", obj.toString() );
//		assertEquals( 302, cli.getResponseCode() );
//		
//		S.sleep( 1000);
//		
//		cli().get( "/api/?msg=checkForSignups");
//		JsonObject json = cli.readJsonObject();
//		json.display();
//		assertEquals( 1, json.getInt( "added") );
//	}

	public void testCheckEmail() throws Exception {
		String wallet = Util.createFakeAddress().toLowerCase();
		String first = wallet.substring( 2, 10);
		
		// without wallet, data should be inserted into signup table
		JsonObject obj = Util.toJson( 
				"first", first,
				"last", "test",
				"email", "peteraspiro+2@gmail.com",
				"utm_source", "test");
		cli().post("/api/signup", obj.toString() );		
		assertEquals( 302, cli.getResponseCode() );  // signup returns 302 for legacy support

		S.sleep( 1000);

		var ar = m_config.sqlQuery( "select * from signup where first = '%s'", first);
		assertEquals( 1, ar.size() );
		
		// with wallet, data should be inserted into users table
		obj.put( "wallet_public_key", wallet);
		cli().post("/api/signup", obj.toString() );		
		assertEquals( 302, cli.getResponseCode() );  // signup returns 302 for legacy support
		
		S.sleep( 1000);

		var ar2 = m_config.sqlQuery( "select * from users where wallet_public_key = '%s' and first_name = '%s'", wallet, first);
		assertEquals( 1, ar2.size() );
	}
}
