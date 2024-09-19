package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import onramp.Onramp;
import onramp.Onramp.KycStatus;
import tw.util.S;

public class OnrampTransaction extends MyTransaction {

	OnrampTransaction(Main main, HttpExchange exchange) {
		super(main, exchange, true);
	}

	public void handleGetQuote() {
		wrap( () -> {
			parseMsg();
			m_walletAddr = m_map.getString( "wallet_public_key"); // used only for debugging

			String currency = m_map.getRequiredString("currency");
			require( Onramp.isValidCurrency( currency), RefCode.INVALID_REQUEST, "The selected currency is invalid");
			
			double amount = m_map.getRequiredDouble( "buyAmt");
			require( amount > 0, RefCode.INVALID_REQUEST, "The buy amount is invalid");
			
			respond( Util.toJson( "recAmt", Onramp.getQuote( currency, amount) ) );
		});
	}

	/** user sends: currency, amount, receive-amount 
	 *  can return: code, message, url */
	public void handleConvert() {
		wrap( () -> {
			parseMsg();
			m_walletAddr = m_map.getWalletAddress("wallet_public_key");
			validateCookie("onramp-convert");
			
			var json = getOrCreateOnrampUser();  // get url, customerId, and status
			
			KycStatus status = json.getEnum( "status", KycStatus.values(), KycStatus.IN_REVIEW);
			if (status.isCompleted() ) {
				String currency = m_map.getRequiredString("currency");
				require( Onramp.isValidCurrency( currency), RefCode.INVALID_REQUEST, "The selected currency is invalid");
				
				double amount = m_map.getRequiredDouble( "buyAmt");
				require( amount > 0, RefCode.INVALID_REQUEST, "The buy amount is invalid");
				
				double receiveAmt = m_map.getRequiredDouble( "recAmt");
				require( receiveAmt > 0, RefCode.INVALID_REQUEST, "The receive amount is invalid");
	
				String onrampId = getorCreateUser().getString( "onramp_id");
				require( S.isNotNull( onrampId), RefCode.INVALID_REQUEST, "No on-ramp id found");
				
				var resp = Onramp.transact( 
						onrampId,
						amount,
						currency,
						m_config.refWalletAddr(),
						receiveAmt);
				
				require( resp.getInt( "code") == 200, RefCode.ONRAMP_FAILED,
					"An error occurred - " + resp.getString( "error") );
	
				respond( Util.toJson( code, 200, Message, "The transaction has been initiated.<br>"
						+ "You will receive an email notifying you when it is completed.") );
			}
			else {
				respond( json
						.append( code, 200)
						.append( Message, "Please KYC with our on-ramp partner"));
			}
		});
	}

	/** once a phone number is linked to the onramp cust id, it can never change,
	 *  so we have to remember id
	 *   
	 * @return fields are url customerId and status */
	private JsonObject getOrCreateOnrampUser() throws Exception {
		var user = getorCreateUser();
		
		String onrampId = user.getString( "onramp_id");  // cust id and phone
		JsonObject json;
		
		// first time?
		if (S.isNull( onrampId) ) {
			String phone = user.getString( "phone");

			require( isValidPhone( phone), RefCode.INVALID_REQUEST, 
					"Please update your user profile to include a valid phone number.\n"
					+ "The required format is: '+##-123456789' where ## is the country code, e.g. '+91-8374827'\n"
					+ "(You can update your profile from the drop-down menu in the upper-right corner.)");

			json = Onramp.getKycUrlFirst( m_walletAddr, phone, m_config.baseUrl() );
			String custId = json.getString( "customerId");
			Util.require( S.isNotNull( custId), "No on-ramp ID was assigned");
			
			m_config.sqlCommand( sql -> sql.insertOrUpdate(
					"users",
					Util.toJson( 
						"wallet_public_key", m_walletAddr.toLowerCase(),
						"onramp_id", custId),
					"where wallet_public_key='%s'",
					m_walletAddr.toLowerCase() ) );
		}
		else {
			json = Onramp.getKycUrlNext( onrampId, m_config.baseUrl() );
			Util.require( json.getString( "customerId").equals( onrampId), "The on-ramp ID has changed" );  //onramp id should not change
		}

		return json;
	}

	private static boolean isValidPhone(String phone) {
		return	phone.startsWith( "+") && 
				phone.indexOf( "-") != -1 && 
				phone.indexOf( " ") == -1;
	}
	
	interface Func {
		void process( String str1, String str2);
	}

	// useless
	void split( String str, String sep, Func func) {
		String[] split = str.split( sep);
		func.process( split[0], split[1]);
	}

}
