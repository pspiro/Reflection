package reflection;

import static reflection.Main.require;

import org.json.simple.JsonObject;

import common.Util;

/** User profile which can be edited by the user. */
public class Profile extends JsonObject {
	static final String fields = "wallet_public_key,first_name,last_name,address,email,phone,pan_number,aadhaar";
	
	public Profile(JsonObject source) {
		copyFrom(source, fields.split(",") );
		
		// wallet address must be lower case because this object is inserted into database
		update( "wallet_public_key", val -> val.toString().toLowerCase() ); // must be lower case because this gets inserted into the db
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
			require( getString(tag).length() <= 100, RefCode.INVALID_USER_PROFILE, "The '%s' entered is invalid", tag);
		}

		// validate pan and aadhaar
		require( aadhaar().replaceAll("-", "").matches( "^\\d{12}$"), RefCode.INVALID_USER_PROFILE, "The Aadhaar entered is invalid"); 
		require( pan().toUpperCase().matches("^[A-Z]{5}[0-9]{4}[A-Z]$"), RefCode.INVALID_USER_PROFILE, "The PAN entered is invalid");
		require( Util.isValidEmail( email() ), RefCode.INVALID_USER_PROFILE, "The email entered is invalid");

		// don't allow < or > in user entry fields
		for (String tag : keySet() ) {
			require( 
					validUserEntry( getString(tag) ), 
					RefCode.INVALID_USER_PROFILE, 
					"The '%s' field contains an invalid character", tag);
		}
	}

	private static boolean validUserEntry(String str) {
		return str.indexOf('<') == -1 && str.indexOf('>') == -1;
	}

	/** return true for testing */
	public boolean skip() {
		return email().equals("test@test.com");
	}

}
//you have to see what they are sending; this check looks backwards