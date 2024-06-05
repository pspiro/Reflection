package testcase;

import org.json.simple.JsonObject;

import common.Util;

public class TestSignup extends MyTestCase {
	public void testSheet() throws Exception {
		String email = "email" + Util.rnd.nextInt();
		
		JsonObject obj = Util.toJson( 
				"first", "first",
				"last", "last",
				"email", email);
		cli().post("/api/signup", obj.toString() );
		assertEquals( 302, cli.getResponseCode() );
		
		cli().get( "/api/?msg=checkForSignups");
		JsonObject json = cli.readJsonObject();
		json.display();
		assertEquals( 1, json.getInt( "added") );
	}
}
