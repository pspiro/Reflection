package reflection;

import static reflection.Main.require;

import org.json.simple.JsonObject;

import common.Util;
import tw.util.S;

public class Profile extends JsonObject {
	public Profile(JsonObject profile) {
		super(profile);
		
		put("wallet_public_key", wallet().toLowerCase() );
	}
	
	/** Always returns lower case */
	String wallet() {
		return getString("wallet_public_key");
	}
	
	String first() {
		return getString("first_name");
	}

	String last() {
		return getString("last_name");
	}

	String address() {
		return getString("address");
	}

	String email() {
		return getString("email");
	}

	String phone() {
		return getString("phone");
	}

	String pan() {
		return getString("pan_number");
	}

	String aadhaar() {
		return getString("aadhaar");
	}
	
	void validate() throws RefException {
		// check for missing fields
		for (String tag : "first_name,last_name,phone".split(",") ) {
			require (has( tag), RefCode.INVALID_USER_PROFILE, "Missing user attribute '%s'", tag);
		}

		// validate fields
		require( Util.isValidAddress(wallet()), RefCode.INVALID_USER_PROFILE, "The wallet '%s' is invalid", wallet() );
		require( aadhaar().length() == 12, RefCode.INVALID_USER_PROFILE, "Aadhaar '%s' is invalid", aadhaar() ); 
		require( pan().length() == 10, RefCode.INVALID_USER_PROFILE, "PAN '%s' is invalid", pan() );
		require( Util.isValidEmail( email() ), RefCode.INVALID_USER_PROFILE, "Email '%s' is invalid", email() );		
	}

	public void checkKyc(boolean smallOrder) throws RefException {
		require( 
				smallOrder || getBool("persona_response") && S.isNotNull( getString("kyc_status") ),
				RefCode.NEED_KYC,
				"KYC is required for this order");

	}
}
