package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import onramp.Onramp;
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

	/** return KYC status and URL */
	public void handleGetKycInfo() {
		wrap( () -> {
			parseMsg();
			m_walletAddr = m_map.getWalletAddress("wallet_public_key");
			validateCookie("onramp-kyc");
			
			var user = getorCreateUser();

			String phone = fixPhone( user.getString( "phone") );
			
			require( isValidPhone( phone), RefCode.INVALID_REQUEST, 
					"Please update your user profile to include a valid phone number.\n"
					+ "The required format is: '+##-123456789' where ## is the country code, e.g. '+91-8374827'");
			
			String onrampId = user.getString( "onramp_id");
			JsonObject json;
			
			// first time?
			if (S.isNull( onrampId) ) {
				json = Onramp.getKycUrl( m_walletAddr, phone);
				String newOnrampId = json.getString( "customerId");
				Util.require( S.isNotNull( newOnrampId), "No on-ramp ID was assigned");
				
				m_config.sqlCommand( sql -> sql.insertOrUpdate(
						"users",
						Util.toJson( 
							"wallet_public_key", m_walletAddr.toLowerCase(),
							"onramp_id", newOnrampId),
						"where wallet_public_key='%s'",
						m_walletAddr.toLowerCase() ) );
			}
			else {
				json = Onramp.getKycUrl( onrampId, m_walletAddr, phone);
				Util.require( json.getString( "customerId").equals( onrampId), "The on-ramp ID has changed" );  //onramp id should not change
				Onramp.getKycUrl( onrampId, m_walletAddr, phone);
			}
				
			respond( json);  // fields are url customerId and status
		});
	}

	public void handleConvert() {
		wrap( () -> {
			parseMsg();
			m_walletAddr = m_map.getWalletAddress("wallet_public_key");
			validateCookie("onramp-convert");
			
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

			respond( Util.toJson( code, 200, Message, "The transaction was accepted") );
		});
	}

	private String fixPhone(String phone) {
		return phone.trim().replaceAll( " ", "-");
	}

	private static boolean isValidPhone(String phone) {
		return phone.startsWith( "+") && phone.indexOf( "-") != -1;
	}
	

}
