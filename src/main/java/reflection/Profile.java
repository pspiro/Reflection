package reflection;

import static reflection.Main.require;

import org.json.simple.JsonObject;

import common.Util;
import tw.util.S;

/** User profile which can be edited by the user. Note that wallet is not part of this */
public class Profile extends JsonObject {
	static String[] fields = "first_name,last_name,address,email,phone,pan_number,aadhaar".split(",");
	
	public Profile(JsonObject source) {
		for (String field : fields) {
			Object val = source.get(field);
			if (val != null) {
				put( field, val);
			}
		}
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
		// check for missing fields or too long fields
		for (String tag : "first_name,last_name,phone".split(",") ) {
			require( has(tag), RefCode.INVALID_USER_PROFILE, "Missing user attribute '%s'", tag);
			//require( getString(tag).getLength() <= 100, "The "
		}

		// validate fields
		require( aadhaar().length() == 12, RefCode.INVALID_USER_PROFILE, "The Aadhaar '%s' is invalid", aadhaar() ); 
		require( pan().length() == 10, RefCode.INVALID_USER_PROFILE, "The PAN '%s' is invalid", pan() );
		require( Util.isValidEmail( email() ), RefCode.INVALID_USER_PROFILE, "The email address '%s' is invalid", email() );

		// don't allow < or > in user entry fields
		for (String tag : fields) {
			require( 
					validUserEntry( getString(tag) ), 
					RefCode.INVALID_USER_PROFILE, 
					"The '%s' field contains an invalid character", tag);
		}
	}

	private static boolean validUserEntry(String str) {
		return str.indexOf('<') == -1 && str.indexOf('>') == -1;
	}

	public void checkKyc(boolean smallOrder) throws RefException {
		require( 
				smallOrder || getBool("persona_response") && S.isNotNull( getString("kyc_status") ),
				RefCode.NEED_KYC,
				"KYC is required for this order");

	}
}
