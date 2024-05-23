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
	
	public void testBigProfile() throws Exception {
		JsonObject json = createBigProfile();
		cli().post("/api/validate-email", json.toString() );
		assert200();

		// test correct code
		String code = Util.input("Enter code:");
		json.put("email_confirmation", code);
		cli().postToJson("/api/update-profile", json.toString() );
		
		JsonObject json2 = new JsonObject();
		json2.put( "cookie", Cookie.cookie);
				
		JsonObject ret = cli().postToJson("/api/get-profile/" + Cookie.wallet, json2.toString() );
		ret.display();

		assert200();
	}		

	static JsonObject createBigProfile() {
		JsonObject json = createValidProfile();
		json.put( "address_1", "nautiluss");
		json.put( "address_1", "lanee");
		json.put( "city", "mamaroneck");
		json.put( "state", "ny");
		json.put( "zip", "10543");
		json.put( "country", "usa");
		json.put( "telegram", "@peterspiro");
		return json;
	}
	
	static JsonObject createValidProfile() {
		JsonObject json = new JsonObject();
		json.put( "wallet_public_key", Cookie.wallet.toLowerCase());
		json.put( "cookie", Cookie.cookie);
		json.put( "first_name", "jammy");
		json.put( "last_name", "sprate");
		json.put( "email", email);
		json.put( "phone", "9149399393");
		json.put( "pan_number", "XXXXX9393Y");
		json.put( "aadhaar", "939393939393");
		return json;
	}

	static JsonObject createValidSkipEmail() {
		JsonObject json = createValidProfile();
		json.put( "email", "test@test.com");
		return json;
	}

	static JsonObject createProfileNC() {
		JsonObject json = createValidProfile();
		json.remove("cookie");
		return json;
	}
}
