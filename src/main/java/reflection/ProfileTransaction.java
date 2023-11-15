package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import tw.util.S;

public class ProfileTransaction extends MyTransaction {
	static HashMap<String,String> mapWalletToCode = new HashMap<>();

	ProfileTransaction(Main main, HttpExchange exch) {
		super(main, exch);
	}

	public void handleGetProfile() {
		wrap( () -> {
			getWalletFromUri(); // read wallet address into m_walletAddr (last token in URI)
			parseMsg();         // read cookie from msg body into m_map
			S.out( "GET PROFILE COOKIE " + m_map.get("cookie") );
			//validateCookie();

			JsonArray ar = m_config.sqlQuery( conn -> conn.queryToJson(
					"select first_name, last_name, address, email, phone, pan_number, aadhaar from users where wallet_public_key = '%s'", 
					m_walletAddr.toLowerCase() ) );
				
			JsonObject obj = ar.size() == 0 
					? new JsonObject() 
					: (JsonObject)ar.get(0);
				
			respond(obj);
		});
	}
	
	/** Send an email with a code to the user; they will enter the code on the profile screen */
	public void validateEmail() {
		wrap( () -> {
            JsonObject data = parseToObject();
			
            String wallet = data.getLowerString( "wallet_public_key");
			require( Util.isValidAddress(wallet), RefCode.INVALID_REQUEST, "The wallet '%s' is invalid", wallet);

			String email = data.getString( "email");
			require( Util.isValidEmail(email), RefCode.INVALID_REQUEST, "The email '%s' is invalid for wallet '%s'", email, wallet);
			
			String code = Util.uin(5);
			out( "Emailing verification code '%s' for wallet '%s' to email '%s'", code, wallet, email);
			
			mapWalletToCode.put( wallet, code); // save code
			
			Main.m_config.sendEmail(
					email,
					"Reflection Verification Code",
					"Your Reflection Verification code is: " + code,
					false);
			
			respondOk();
		});
	}

	public void handleUpdateProfile() {
		wrap( () -> {
			parseMsg();
			m_walletAddr = m_map.getRequiredParam("wallet_public_key");
			S.out( "UPDATE PROFILE COOKIE " + m_map.get("cookie") );
			//validateCookie();

			Profile profile = new Profile( m_map.obj() );
			profile.trim(); // trim spaces since this data was entered by the user
			profile.validate();
			
			String walletKey = m_walletAddr.toLowerCase();
			
			// if email has changed, they must submit a valid verification code from the validateEmail() message
			if (!profile.email().equalsIgnoreCase( getExistingEmail(walletKey) ) ) {
				require( m_map.getString("email_confirmation").equalsIgnoreCase(mapWalletToCode.get(walletKey) ),
						RefCode.INVALID_USER_PROFILE,
						"The email verification code is incorrect");
				mapWalletToCode.remove(walletKey); // remove only if there is a match so they can try again
			}

			// insert or update record in users table
			m_config.sqlCommand( conn -> conn.insertOrUpdate("users", profile, "wallet_public_key = '%s'", walletKey) );
			respondOk();
		});
	}

	private String getExistingEmail(String walletAddr) throws Exception {
		JsonArray res = Main.m_config.sqlQuery( conn -> conn.queryToJson("select email from users where wallet_public_key = '%s'", walletAddr) );
		return res.size() > 0
				? res.get(0).getString("email")
				: "";
	}	

}
