package testcase;

import java.util.Random;

import org.json.simple.JsonObject;

import common.Util;
import reflection.RefCode;

public class TestProfile extends MyTestCase {
	static String email = "jack" + ((new Random()).nextInt()) + "@gmail.com";
	
	public void testGetProfile() throws Exception {
		JsonObject json = createValidProfile();
		cli().post("/api/get-profile/" + Cookie.wallet, json.toString() );
		assert200();
		
		// get-profile requires cookie
		json.remove("cookie");
		cli().post("/api/get-profile/" + Cookie.wallet, json.toString() );
		assertEquals( RefCode.VALIDATION_FAILED, cli.getRefCode() );
	}
	
	public void testUpdateProfile() throws Exception {
		JsonObject json = createValidProfile();
		cli().post("/api/validate-email", json.toString() );
		assert200();

		// test wrong code
		json.put("email_confirmation", "wrong code");
		cli().post("/api/update-profile", json.toString() );
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( RefCode.INVALID_USER_PROFILE, cli.getRefCode() );
		
		// test correct code
		String code = Util.input("Enter code:");
		json.put("email_confirmation", code);
		cli().post("/api/update-profile", json.toString() );
		assert200();

		// missing first name 
		json = createValidProfile();
		json.remove( "first_name");
		cli().post("/api/update-profile", json.toString() );
		assertEquals( RefCode.INVALID_USER_PROFILE, cli.getRefCode() );

		// missing last name 
		json = createValidProfile();
		json.remove( "last_name");
		cli().post("/api/update-profile", json.toString() );
		assertEquals( RefCode.INVALID_USER_PROFILE, cli.getRefCode() );

		// invalid email 
		json = createValidProfile();
		json.put( "email", "junk@email");
		cli().post("/api/update-profile", json.toString() );
		assertEquals( RefCode.INVALID_USER_PROFILE, cli.getRefCode() );

		// missing phone 
		json = createValidProfile();
		json.remove( "phone");
		cli().post("/api/update-profile", json.toString() );
		assertEquals( RefCode.INVALID_USER_PROFILE, cli.getRefCode() );

		// missing missing aadhaar 
		json = createValidProfile();
		json.remove( "aadhaar");
		cli().post("/api/update-profile", json.toString() );
		assertEquals( RefCode.INVALID_USER_PROFILE, cli.getRefCode() );

		// wrong aadhaar 
		json = createValidProfile();
		json.put( "aadhaar", "junk");
		cli().post("/api/update-profile", json.toString() );
		assertEquals( RefCode.INVALID_USER_PROFILE, cli.getRefCode() );

		// missing pan
		json = createValidProfile();
		json.remove( "pan_number");
		cli().post("/api/update-profile", json.toString() );
		assertEquals( RefCode.INVALID_USER_PROFILE, cli.getRefCode() );

		// wrong pan
		json = createValidProfile();
		json.put( "pan_number", "junk");
		cli().post("/api/update-profile", json.toString() );
		assertEquals( RefCode.INVALID_USER_PROFILE, cli.getRefCode() );

		// missing cookie
		json = createValidProfile();
		json.remove("cookie");
		cli().post("/api/update-profile", json.toString() );
		assertEquals( RefCode.VALIDATION_FAILED, cli.getRefCode() );
	}

	private JsonObject createValidProfile() {
		JsonObject json = new JsonObject();
		json.put( "wallet_public_key", Cookie.wallet);
		json.put( "cookie", Cookie.cookie);
		json.put( "first_name", "jack");
		json.put( "last_name", "sprat");
		json.put( "email", email);
		json.put( "phone", "9149399393");
		json.put( "pan_number", "XXXXX9393Y");
		json.put( "aadhaar", "939393939393");
		return json;
	}
}
