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

				require( isValidPhone( phone), RefCode.INVALID_REQUEST, 
						"Please update your user profile to include a valid phone number.\n"
						+ "The required format is: '+##-123456789' where ## is the country code, e.g. '+91-8374827'\n"
						+ "(You can update your profile from the drop-down menu in the upper-right corner.)");

				var json = Onramp.getKycUrlFirst( m_walletAddr, phone, m_config.baseUrl() );
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
						.append( Message, "Please verify your identity with our on-ramp partner") );
				
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
