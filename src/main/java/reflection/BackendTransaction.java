package reflection;

import static reflection.Main.log;
import static reflection.Main.require;

import java.sql.Timestamp;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ib.client.Contract;
import com.sun.net.httpserver.HttpExchange;

import fireblocks.Accounts;
import fireblocks.Busd;
import fireblocks.Erc20;
import fireblocks.Rusd;
import fireblocks.StockToken;
import json.MyJsonArray;
import json.MyJsonObject;
import positions.MoralisServer;
import tw.util.S;
import util.LogType;

/** This class handles events from the Frontend, simulating the Backend */
public class BackendTransaction extends MyTransaction {

	public BackendTransaction(Main main, HttpExchange exch) {
		super(main,exch);
	}
	
	/** Used by the portfolio section on the dashboard
	 *  We're returning the token positions from the blockchain, not IB positions */
	public void handleReqPositions() {
		wrap( () -> {
			// get wallet address (last token in URI)
			String address = Util.getLastToken(m_uri, "/");
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
			
			respond(retVal);
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
	public void handleGetStockWithPrice() {
		wrap( () -> {
			String conidStr = Util.getLastToken(m_uri, "/");
			require( !conidStr.equals("undefined"), RefCode.INVALID_REQUEST, "get-stock-with-price should not be called with 'undefined' conid");
			int conid = Integer.valueOf(conidStr);
			
			Stock stock = m_main.getStock(conid);
			
			Contract contract = new Contract();
			contract.conid(conid);
			contract.exchange( m_main.getExchange( conid) );
			
			insideAnyHours( contract, inside -> {
				stock.put( "exchangeStatus", inside ? "open" : "closed");
				respond(stock);
			});
			
			// if we timed out, respond with the prices anyway
			setTimer( Main.m_config.timeout(), () -> {
				log( LogType.TIMEOUT, "handleGetStockWithPrice timed out");
				respond(stock);
			});
		});
	}
	
	/** Backend-style msg; conid is last parameter */
	public void handleGetPrice() {
		wrap( () -> {
			int conid = Integer.valueOf( Util.getLastToken(m_uri, "/") );
			returnPrice(conid);
		});
	}
	
	/** Redeem (sell) RUSD */ 
	public void handleRedeem() {
		wrap( () -> {
			String walletAddr = Util.getLastToken(m_uri, "/");
			require( Util.isValidAddress(walletAddr), RefCode.INVALID_REQUEST, "Wallet address is invalid");
			
			validateCookie(walletAddr);

			Rusd rusd = Main.m_config.rusd();
			Busd busd = Main.m_config.busd();

			setTimer( Main.m_config.timeout(), () -> timedOut( "redemption request timed out") );

			double rusdPos = rusd.getPosition(walletAddr);
			require( rusdPos > 0, RefCode.INSUFFICIENT_FUNDS, "No RUSD in user wallet to redeem");
			
			log( LogType.REDEEM, "%s is selling %s RUSD", walletAddr, rusdPos);

			double busdPos = busd.getPosition( Accounts.instance.getAddress("RefWallet") );
			if (busdPos >= rusdPos) {
				rusd.sellRusd(walletAddr, Main.m_config.busd(), rusdPos)
					.waitForHash();
				
				// QUESTION: can we send back a partial response before the hash is ready using chunks or WebSockets?
				
				respond( code, RefCode.OK);  // wait for completion. pas 
			}
			else {
				String str = String.format( 
						"Insufficient stablecoin in RefWallet for RUSD redemption  \nwallet=%s  requested=%s  have=%s  need=%s",
						walletAddr, rusdPos, busdPos, (rusdPos - busdPos) );
				alert( "MOVE FUNDS NOW TO REDEEM RUSD", str);
				throw new RefException( RefCode.INSUFFICIENT_FUNDS, str);
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

	/** Top-level method */
	public void handleGetType2Config() {
		wrap( () -> {
			parseMsg();
			String key = m_map.get("key");
			if (key != null) {
				respond( key, m_main.type2Config().getString(key) );
			}
			else {
				respond(m_main.type2Config().toJsonObj() );
			}
		});
	}

	public void handleReqCryptoTransactions(HttpExchange exch) {
		wrap( () -> {
			parseMsg();
			
			String wallet = m_map.get("wallet_public_key");
			Main.require( S.isNull(wallet) || Util.isValidAddress(wallet), RefCode.INVALID_REQUEST, "Wallet address is invalid: %s", wallet);
			
			String where = S.isNotNull(wallet) 
					? String.format( "where lower(wallet_public_key)='%s'", wallet.toLowerCase() )
					: "";
			respond(trim(m_main.sqlConnection().queryToJson( 
					"select * from crypto_transactions %s order by created_at desc", 
					where) ) );
		});
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private JSONArray trim(JSONArray json) {
		json.forEach( obj -> {
			((HashMap)obj).remove("created_at");
			((HashMap)obj).remove("updated_at");
		});
		return json;
	}

	public void handleGetUserByWallet() {
		wrap( () -> {
			String wallet = Util.getLastToken(m_uri, "/");
			Main.require( S.isNull(wallet) || Util.isValidAddress(wallet), RefCode.INVALID_REQUEST, "Wallet address is invalid: %s", wallet);
			
			JSONArray ar = m_main.sqlConnection().queryToJson(
					"select * from users where lower(wallet_public_key) = '%s'", 
					wallet.toLowerCase() );
			Main.require( ar.size() == 1, RefCode.INVALID_REQUEST, "Wallet address %s not found", wallet);
			
			respond( (JSONObject)trim( ar).get(0) );
		});
	}

	public void handleWalletUpdate() {
		S.out( "  ignoring");
		respondOk();
	}
	
}
