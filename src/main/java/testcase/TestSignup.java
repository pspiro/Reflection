package testcase;

import org.json.simple.JsonObject;

import common.Util;

public class TestSignup extends MyTestCase {
	public void testFailName() throws Exception {
		JsonObject obj = Util.toJson( 
				"email", "838383838",
				"phone", "838383838", 
				"wallet_public_key", "0xb95bf9C71e030FA3D8c0940456972885DB60843F");
		cli().postToJson("/api/signup", obj.toString() ).display();
		assertEquals( cli.getResponseCode(), 400);
		startsWith( "Please enter your", cli.getMessage() );
	}
	public void testFailEmail() throws Exception {
		JsonObject obj = Util.toJson( 
				"name", "bob",
				"phone", "838383838", 
				"wallet_public_key", "0xb95bf9C71e030FA3D8c0940456972885DB60843F");
		cli().postToJson("/api/signup", obj.toString() ).display();
		assertEquals( cli.getResponseCode(), 400);
		startsWith( "Please enter your", cli.getMessage() );
	}
	public void testFailWallet() throws Exception {
		JsonObject obj = Util.toJson( 
				"name", "bob",
				"email", "838383838",
				"phone", "838383838", 
				"wallet_public_key", "0xb95bf9C71e030FA3D8c0940456972885DBwwwwww608");
		cli().postToJson("/api/signup", obj.toString() ).display();
		assertEquals( 400, cli.getResponseCode() );
	}
	public void testSuccess() throws Exception {
		JsonObject obj = Util.toJson( 
				"name", "bob",
				"email", "838383838",
				"phone", "838383838", 
				"wallet_public_key", "0xb95bf9C71e030FA3D8c0940456972885DB60843F");
		cli().postToJson("/api/signup", obj.toString() ).display();
		assert200();

		obj = Util.toJson( 
				"name", "bob",
				"email", "838383838",
				"phone", "838383838", 
				"wallet_public_key", "");
		cli().postToJson("/api/signup", obj.toString() ).display();
		assert200();
	}
}
