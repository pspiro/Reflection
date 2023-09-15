package testcase;

import java.util.Random;

import org.json.simple.JsonObject;

import fireblocks.TestSellRusd;
import reflection.RefCode;

public class TestValidateEmail extends MyTestCase {
	public void testFailure() throws Exception {
		JsonObject json;
		
		json = createValidProfile();
		json.put( "wallet_public_key", "junk");
		cli().post("/api/update-profile", json.toString() );
		assertEquals( RefCode.INVALID_USER_PROFILE, cli.getRefCode() );
		
		json = createValidProfile();
		json.remove( "first_name");
		cli().post("/api/update-profile", json.toString() );
		assertEquals( RefCode.INVALID_USER_PROFILE, cli.getRefCode() );

		json = createValidProfile();
		json.remove( "last_name");
		cli().post("/api/update-profile", json.toString() );
		assertEquals( RefCode.INVALID_USER_PROFILE, cli.getRefCode() );

		json = createValidProfile();
		json.put( "email", "junk@email");
		cli().post("/api/update-profile", json.toString() );
		assertEquals( RefCode.INVALID_USER_PROFILE, cli.getRefCode() );

		json = createValidProfile();
		json.remove( "phone");
		cli().post("/api/update-profile", json.toString() );
		assertEquals( RefCode.INVALID_USER_PROFILE, cli.getRefCode() );

		json = createValidProfile();
		json.put( "aadhaar", "junk");
		cli().post("/api/update-profile", json.toString() );
		assertEquals( RefCode.INVALID_USER_PROFILE, cli.getRefCode() );

		json = createValidProfile();
		json.put( "pan_number", "junk");
		cli().post("/api/update-profile", json.toString() );
		assertEquals( RefCode.INVALID_USER_PROFILE, cli.getRefCode() );

		cli().post("/api/update-profile", createValidProfile().toString() );
		assertEquals( RefCode.INVALID_REQUEST, cli.getRefCode() );  // wrong validation code
	}

	public void testSuccess() throws Exception {
		JsonObject json = createValidProfile();
		cli().post("/api/validate-email", json.toString() );

		// test wrong code
		json.put("email_confirmation", "wrong code");
		cli().post("/api/update-profile", json.toString() );
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( RefCode.INVALID_REQUEST, cli.getRefCode() );
		
		// test correct code
		String code = TestSellRusd.input("Enter code:");
		json.put("email_confirmation", code);
		cli().post("/api/update-profile", json.toString() );
		assert200();
	}

	private JsonObject createValidProfile() {
		JsonObject json = new JsonObject();
		json.put( "wallet_public_key", Cookie.wallet);
		json.put( "first_name", "jack");
		json.put( "last_name", "sprat");
		json.put( "email", "jack" + ((new Random()).nextInt()) + "@gmail.com" );
		json.put( "phone", "9149399393");
		json.put( "aadhaar", "939393939393");
		json.put( "pan_number", "9393939393");
		return json;
	}
}
