package reflection;

import com.sun.net.httpserver.HttpExchange;

import common.Util;

public class OnrampTransaction extends MyTransaction {

	OnrampTransaction(Main main, HttpExchange exchange) {
		super(main, exchange, true);
	}

	public void handleGetQuote() {
		wrap( () -> {
			var req = parseToObject();
			req.display();
			
			respond( Util.toJson( "recAmt", 400.25) );
		});
	}

	public void handleConvert() {
		wrap( () -> {
			parseMsg();
			m_walletAddr = m_map.getWalletAddress("wallet_public_key");
			validateCookie("order");
			
			respondOk();

		});
	}
	
	

}
