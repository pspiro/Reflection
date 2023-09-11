package testcase;

import org.json.simple.JsonObject;

import common.Util;
import fireblocks.TestSellRusd;

public class TestValidateEmail extends MyTestCase {
	public void testFailure() throws Exception {
		// invalid wallet
		JsonObject obj = new JsonObject();
		obj.put( "wallet_public_key", "abc");
		obj.put( "email", "peter@briscoinvestments.com"); 
		cli().post("/api/validate-email", obj.toString() );
		assertEquals( 400, cli.getResponseCode() );

		// invalid email
		obj = new JsonObject();
		obj.put( "wallet_public_key", Cookie.wallet);
		obj.put( "email", "peter@abc"); 
		cli().post("/api/validate-email", obj.toString() );
		assertEquals( 400, cli.getResponseCode() );
	}

	public void testSuccess() throws Exception {
		JsonObject json = new JsonObject();
		json.put("wallet_public_key",  Cookie.wallet);
		json.put("email", "peteraspiro@gmail.com");
		cli().post("/api/validate-email", json.toString() );
		
		json.put("email", Util.uid(5) + "@abc.com");  // create new email address
		json.put("email_confirmation", "wrong code");
		cli().post("/api/update-profile", json.toString() );
		assertEquals( 400, cli.getResponseCode() );
		
		String code = TestSellRusd.input("Enter code:");
		json.put("email_confirmation", code);
		cli().post("/api/update-profile", json.toString() );
		assert200();
		
	}
}
