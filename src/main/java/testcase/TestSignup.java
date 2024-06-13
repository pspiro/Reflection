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
		JsonObject obj = Util.toJson( 
				"first", "jack",
				"last", "sprat",
				"email", "peteraspiro+2@gmail.com");
		cli().post("/api/signup", obj.toString() );
		assertEquals( 302, cli.getResponseCode() );
	}
}
