package testcase;

import org.json.simple.JsonObject;

import common.Util;

public class TestSignup extends MyTestCase {
	public void test() throws Exception {
		JsonObject obj = Util.toJson( 
				"name", "wwwwwwwwwwwwwwwwwwww wwwwwwwwwwwwwwwwwwww",
				"email", "83838383838383838383838383838383838383838383838383",
				"phone", "8383838383838383", 
				"wallet_public_key", "0xb95bf9C71e030FA3D8c0940456972885DB60843F");
		cli().postToJson("/api/signup", obj.toString() ).display();
		assert200();
	}
}
