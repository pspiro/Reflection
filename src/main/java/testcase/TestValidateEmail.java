package testcase;

import org.json.simple.JsonObject;

import tw.util.S;

public class TestValidateEmail extends MyTestCase {
	public void test() throws Exception {
		JsonObject obj = new JsonObject();
		obj.put( "wallet_public_key", Cookie.wallet);
		obj.put( "email", "peter@briscoinvestments.com"); 
		cli().post("/api/validate-email", obj.toString() );
		S.out( cli.getRefCode() + " " + cli.getMessage() );
		// error
	}
}
