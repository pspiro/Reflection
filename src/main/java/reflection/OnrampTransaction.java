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

	public void handleConvert() {
		wrap( () -> {
			parseMsg();
			m_walletAddr = m_map.getWalletAddress("wallet_public_key");
			validateCookie("order");
			
			String currency = m_map.getRequiredString("currency");
			require( Onramp.isValidCurrency( currency), RefCode.INVALID_REQUEST, "The selected currency is invalid");
			
			double amount = m_map.getRequiredDouble( "buyAmt");
			require( amount > 0, RefCode.INVALID_REQUEST, "The buy amount is invalid");
			
			double receiveAmt = m_map.getRequiredDouble( "recAmt");
			require( receiveAmt > 0, RefCode.INVALID_REQUEST, "The receive amount is invalid");

			var user = getorCreateUser();

			String phone = fixPhone( user.getString( "phone") );
			
			require( isValidPhone( phone), RefCode.INVALID_REQUEST, 
					"Please update your user profile to include a valid phone number.\n"
					+ "The required format is: '+CC-123456789' where cc is the country code");
			
			String onrampId = user.getString( "onramp_id");
			JsonObject json;
			
			// first time?
			if (S.isNull( onrampId) ) {
				json = Onramp.getKycUrl( m_walletAddr, phone);
				String newOnrampId = json.getString( "customerId");
				Util.require( S.isNotNull( onrampId), "No on-ramp ID was assigned");
				
				m_config.sqlCommand( sql -> sql.insertOrUpdate(
						"users",
						Util.toJson( 
							"wallet_public_key", m_walletAddr.toLowerCase(),
							"onramp_id", newOnrampId),
						"where wallet_public_key='%s'",
						m_walletAddr.toLowerCase() ) );
			}
			else {
				json = Onramp.getKycUrl( m_walletAddr, phone);
				Util.require( json.getString( "customerId").equals( onrampId), "The on-ramp ID has changed" );  //onramp id should not change
				Onramp.getKycUrl( onrampId, m_walletAddr, phone);
			}
				
			respond( json);  // fields are url customerId and status
		});
	}

	private String fixPhone(String phone) {
		return phone.trim().replaceAll( " ", "-");
	}

	private static boolean isValidPhone(String phone) {
		return phone.startsWith( "+") && phone.indexOf( "-") != -1;
	}
	

}
