package reflection;

import static reflection.Main.require;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import onramp.Onramp;

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

			var phone = fixPhone( getorCreateUser().getString( "phone") );
			
			require( isValidPhone( phone), RefCode.INVALID_REQUEST, 
					"Please update your user profile to include a valid phone number.\n"
					+ "The required format is: '+cc-123456789' where cc is the country code");
			
			respond( Util.toJson( "url", Onramp.getKycUrl( m_walletAddr, phone)) );
		});
	}

	private String fixPhone(String phone) {
		return phone.trim().replaceAll( " ", "-");
	}

	private static boolean isValidPhone(String phone) {
		return phone.startsWith( "+") && phone.indexOf( "-") != -1;
	}
	

}
