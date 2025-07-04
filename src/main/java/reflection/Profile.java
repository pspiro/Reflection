package reflection;

import static reflection.Main.require;

import org.json.simple.JsonObject;

import common.Util;

/** User profile which can be edited by the user. */
public class Profile extends JsonObject {
	static final String fields = "wallet_public_key,first_name,last_name,address_1,address_2,city,state,zip,country,telegram,email,phone,pan_number,aadhaar";
	
	public Profile(JsonObject source) {
		copyFrom(source, fields.split(",") );
		
		// wallet address must be lower case because this object is inserted into database
		update( "wallet_public_key", val -> val.toString().toLowerCase() ); // must be lower case because this gets inserted into the db
		update( "email", val -> val.toString().toLowerCase() ); // email is standardized on lower case
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

	public boolean isValid() {
		try {
			validate();
			return true;
		}
		catch( Exception e) {
			return false;
		}
	}
	
	public void validate() throws RefException {
		// check for missing fields or too long fields
		for (String tag : "first_name,last_name,phone,email".split(",") ) {
			require( has(tag), RefCode.INVALID_USER_PROFILE, "Missing user attribute '%s'", tag);
			require( getString(tag).length() <= 100, RefCode.INVALID_USER_PROFILE, "The '%s' entered is invalid", tag);
		}

		// validate pan and aadhaar
//		require( Util.isValidAadhaar( aadhaar() ), RefCode.INVALID_USER_PROFILE, "The Aadhaar entered is invalid"); 
//		require( Util.isValidPan( pan() ), RefCode.INVALID_USER_PROFILE, "The PAN entered is invalid");
		require( Util.isValidEmail( email() ), RefCode.INVALID_USER_PROFILE, "The email entered is invalid");

		// don't allow < or > in user entry fields
		for (String tag : keySet() ) {
			require( 
					validUserEntry( getString(tag) ), 
					RefCode.INVALID_USER_PROFILE, 
					"The '%s' field contains an invalid character", tag);
		}
	}

	public void validatePhone() throws RefException {
		require(
				OnrampTransaction.isValidPhone( getString( "phone") ), 
				RefCode.INVALID_USER_PROFILE, 
				"The phone number is invalid. Please use the following format where the first set of digits is the country code:\n\n##-##########"); 
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