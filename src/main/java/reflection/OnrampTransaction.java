package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import onramp.Onramp;
import onramp.Onramp.KycStatus;
import tw.util.S;
import util.LogType;

public class OnrampTransaction extends MyTransaction {
	static String KycMessage = "You will now be redirected to our on-ramp partner to verify your identiy. When completed, please come back to this screen and resubmit your request.";

	OnrampTransaction(Main main, HttpExchange exchange) {
		super(main, exchange, true);
	}

	public void handleGetQuote() {
		wrap( () -> {
			parseMsg();
			m_walletAddr = m_map.getWalletAddress("wallet_public_key");  // used in jlog

			String currency = m_map.getRequiredString("currency");
			require( Onramp.isValidCurrency( currency), RefCode.INVALID_REQUEST, "The selected currency is invalid");
			
			double buyAmt = m_map.getRequiredDouble( "buyAmt");
			require( buyAmt > 0, RefCode.INVALID_REQUEST, "The buy amount is invalid");
			
			double quote = Onramp.getQuote( currency, buyAmt);
			respond( Util.toJson( "recAmt", quote) );
			
			jlog( LogType.ONRAMP, Util.toJson( 
					"type", "quote",
					"currency", currency,
					"buyAmt", buyAmt,
					"recAmt", quote) );
		});
	}

	/** user sends: currency, amount, receive-amount 
	 *  can return: code, message, url */
	public void handleConvert() {
		wrap( () -> {
			parseMsg();
			m_walletAddr = m_map.getWalletAddress("wallet_public_key");
			validateCookie("onramp-convert");
			
			var user = getorCreateUser();
			String onrampId = user.getString( "onramp_id");  // cust id and phone

			// first time?
			if (S.isNull( onrampId) ) {
				String phone = user.getString( "phone");

				require( isValidPhone( phone), RefCode.INVALID_USER_PROFILE, 
						"Please update your user profile to include a valid phone number.\n\n"
						+ "The required format is: '##-########'\n\nwhere the first set of digits is the country code");

				var json = Onramp.getKycUrlFirst( m_walletAddr, format( phone), m_config.baseUrl() );
				String custId = json.getString( "customerId");
				Util.require( S.isNotNull( custId), "No on-ramp ID was assigned");
				
				m_config.sqlCommand( sql -> sql.insertOrUpdate(
						"users",
						Util.toJson( 
							"wallet_public_key", m_walletAddr.toLowerCase(),
							"onramp_id", custId),
						"where wallet_public_key='%s'",
						m_walletAddr.toLowerCase() ) );
				
				respond( json
						.append( code, RefCode.OK)
						.append( Message, KycMessage) );
				
				jlog( LogType.ONRAMP, Util.toJson( "type", "order/KYC part 1") );

				alert( "User started onramp KYC", 
						String.format( "%s %s", user.getString( "first_name"), user.getString( "last_name") ) );
			}
			
			// we have ID; KYC already completed?
			else if (isCompleted( Onramp.getKycStatus( onrampId) ) ) {
				String currency = m_map.getRequiredString("currency");
				require( Onramp.isValidCurrency( currency), RefCode.INVALID_REQUEST, "The selected currency is invalid");
				
				double buyAmt = m_map.getRequiredDouble( "buyAmt");
				require( buyAmt > 0, RefCode.INVALID_REQUEST, "The buy amount is invalid");
				
				double receiveAmt = m_map.getRequiredDouble( "recAmt");
				require( receiveAmt > 0, RefCode.INVALID_REQUEST, "The receive amount is invalid");
	
				// submit order
				var resp = Onramp.transact( 
						onrampId,
						buyAmt,
						currency,
						m_config.refWalletAddr(),
						receiveAmt);
				
				require( resp.getInt( "code") == 200, RefCode.ONRAMP_FAILED,
					"An on-ramp error occurred - " + resp.getString( "error") );
	
				respond( Util.toJson( code, 200, Message, "The transaction has been initiated.<br>"
						+ "You will receive an email notifying you when it is completed.") );
				
				jlog( LogType.ONRAMP, Util.toJson(
						"type", "order/place",
						"currency", currency,
						"buyAmt", buyAmt,
						"recAmt", receiveAmt) );
				
				alert( "USER SUBMITTED ONRAMP ORDER", 
						String.format( "%s %s", user.getString( "first_name"), user.getString( "last_name") ) );
			}
			
			// continue with subsequent KYC
			else {
				var json = Onramp.getKycUrlNext( onrampId, m_config.baseUrl() );
				Util.require( json.getString( "customerId").equals( onrampId), "The on-ramp ID has changed" );  //onramp id should not change

				respond( json
						.append( code, RefCode.OK)
						.append( Message, "Please continue your KYC with our on-ramp partner"));

				jlog( LogType.ONRAMP, Util.toJson( "type", "order/KYC part 2") );
			}
		});
	}
	
	private boolean isCompleted(String status) {
		return Util.equals( status.toUpperCase(), 
				"BASIC_KYC_COMPLETED", 
				"INTERMEDIATE_KYC_COMPLETED", 
				"ADVANCE_KYC_COMPLETED");
	}

	public static void main(String[] args) throws Exception {
		S.out( isValidPhone( "-9393939393") );
		S.out( isValidPhone( "2-22224") );
		S.out( isValidPhone( "2 -22224") );
		S.out( isValidPhone( "2- 22224") );
		S.out( isValidPhone( "34-8282") );
		S.out( isValidPhone( "222-9393939") );
		S.out( isValidPhone( "+939-939393") );
		S.out( isValidPhone( "+9349-939393") );
		
		S.out( format( "2-838383"));
		S.out( format( "+2 -838383"));
	}
	
	/** add the required '+' sign if necessary, strip spaces */
	static String format( String phone) {
		phone = strip( phone);
		return phone.startsWith( "+") ? phone : "+" + phone;
	}
	
	static boolean isValidPhone(String phone) {
		String[] toks = strip(phone).split( "-");
		return toks.length == 2 && toks[0].length() >= 1 && toks[0].length() <= 4; 
	}
	
	private static String strip(String phone) {
		return phone.replaceAll( " ", "");
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
