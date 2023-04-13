package reflection;

import static reflection.Main.log;
import static reflection.Main.require;

import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sun.net.httpserver.HttpExchange;

import fireblocks.Accounts;
import fireblocks.Busd;
import fireblocks.Erc20;
import fireblocks.Rusd;
import fireblocks.StockToken;
import json.MyJsonArray;
import json.MyJsonObject;
import positions.MoralisServer;
import util.LogType;

/** This class handles events from the Frontend, simulating the Backend */
public class BackendTransaction extends MyTransaction {

	public BackendTransaction(Main main, HttpExchange exch) {
		super(main,exch);
	}
	
/** Msg received directly from Frontend via nginx */
	public void backendOrder(boolean whatIf) {
		wrap( () -> {
			require( "POST".equals(m_exchange.getRequestMethod() ), RefCode.INVALID_REQUEST, "order and check-order must be POST"); 
			
			parseMsg();

	   		// some should be written to the log file
	        // String = m_map.get("symbol": "META",
			// String = m_map.getRequiredParam("currency");
			// String smartcontractid = m_map.getRequiredParam("smartcontractid");
			// double spread = m_map.getRequiredDouble("spread"); //??????????
			// double commission = m_map.getRequiredDouble("commission");

//			int conid = m_map.getRequiredInt("conid");
//			double quantity = m_map.getRequiredDouble("quantity");
//			double price = m_map.getRequiredDouble("price");

			String side = m_map.getRequiredParam("action");
			require( side == "buy" || side == "sell", RefCode.INVALID_REQUEST, "Side must be 'buy' or 'sell'");
			m_map.put( "side", side);

			String wallet = m_map.getRequiredParam("wallet_public_key");
			m_map.put( "wallet", wallet); // you can remove this when orders are no longer being passed through the back-end

			order(whatIf, false);
		});
    }

	/** Used by the portfolio section on the dashboard
	 *  We're returning the token positions from the blockchain, not IB positions */
	public void handleReqPositions(String uri) {
		wrap( () -> {
			// get wallet address (last token in URI)
			String address = Util.getLastToken(uri, "/");
			require( Util.isValidAddress(address), RefCode.INVALID_REQUEST, "Wallet address is invalid");
			
			// query positions from Moralis
			setTimer( Main.m_config.timeout(), () -> timedOut( "request for token positions timed out") );
			MyJsonArray positions = MoralisServer.reqPositions(address);
			
			JSONArray retVal = new JSONArray();
			
			for (MyJsonObject position : positions) {
				HashMap stock = m_main.getStockByTokAddr( position.getString("token_address") );

				double balance = Erc20.fromBlockchain(
						(String)position.getString("balance"), 
						StockToken.stockTokenDecimals);
				
				if (stock != null && balance >= Main.m_config.minTokenPosition() ) {
					JSONObject resp = new JSONObject();
					resp.put("conId", stock.get("conid") );
					resp.put("symbol", stock.get("symbol") );
					resp.put("price", getPrice(stock) );
					resp.put("quantity", balance); 
					retVal.add(resp);
				}
			}
			
			respond( new Json(retVal) );
		});
	}

	/** Handle a Backend-style event. Conid is last parameter
	 * 
	 * @return 
		{
		"smartcontractid": "0xd3383F039bef69A65F8919e50d34c7FC9e913e20",
		"symbol": "IBM",
		"ask": 128.78,
		"description": "International Business Machines",
		"conid": "8314",
		"exchange": "SMART",
		"type": "Stock",
		"bid": 128.5
		}
	 */
	public void handleGetStockWithPrice(String uri) {
		wrap( () -> {
			String last = Util.getLastToken(uri, "/");
			require( !last.equals("undefined"), RefCode.INVALID_REQUEST, "get-stock-with-price should not be called with 'undefined' conid");
			int conid = Integer.valueOf(last);
			respond( new Json( m_main.getStock(conid) ) );
		});
	}
	
	/** Backend-style msg; conid is last parameter */
	public void handleGetPrice(String uri) {
		wrap( () -> {
			int conid = Integer.valueOf( Util.getLastToken(uri, "/") );
			returnPrice(conid);
		});
	}
	
	/** Redeem (sell) RUSD 
	 * @param uri */
	public void handleRedeem(String uri) {
		wrap( () -> {
			String userAddr = Util.getLastToken(uri, "/");
			require( Util.isValidAddress(userAddr), RefCode.INVALID_REQUEST, "Wallet address is invalid");

			Rusd rusd = Main.m_config.newRusd();
			Busd busd = Main.m_config.newBusd();

			setTimer( Main.m_config.timeout(), () -> timedOut( "redemption request timed out") );

			double rusdPos = rusd.reqPosition(userAddr);
			require( rusdPos > 0, RefCode.INSUFFICIENT_FUNDS, "No RUSD in user wallet to redeem");
			
			log( LogType.REDEEM, "%s is selling %s RUSD", userAddr, rusdPos);

			double busdPos = busd.reqPosition( Accounts.instance.getAddress("RefWallet") );
			if (busdPos >= rusdPos) {
				rusd.sellRusd(userAddr, Main.m_config.newBusd(), rusdPos);
				
				respond( code, RefCode.OK);  // wait for completion. pas 
			}
			else {
				String str = String.format( 
						"Insufficient stablecoin in RefWallet for RUSD redemption  \nwallet=%s  requested=%s  have=%s  need=%s",
						userAddr, rusdPos, busdPos, (rusdPos - busdPos) );
				alert( "MOVE FUNDS NOW TO REDEEM RUSD", str);
				throw new Exception( str);  // will create log entry with ERROR code
			}
		});
	}

	private static double getPrice(HashMap stock) {
		Double bid = (Double)stock.get("bid");
		Double ask = (Double)stock.get("ask");
		
		if (bid != null && ask != null) {
			return (bid + ask) / 2;
		}
		if (bid != null) {
			return bid;
		}
		if (ask != null) {
			return ask;
		}
		return 0;
	}

}
