package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.json.simple.parser.JSONParser;

import com.ib.client.Contract;
import com.sun.net.httpserver.HttpExchange;

import common.Util;
import fireblocks.Accounts;
import fireblocks.Busd;
import fireblocks.Fireblocks;
import fireblocks.Rusd;
import fireblocks.Transfer;
import positions.MoralisServer;
import positions.Wallet;
import reflection.Config.Tooltip;
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
			String walletAddr = getWalletFromUri();
			
			// query positions from Moralis
			setTimer( m_config.timeout(), () -> timedOut( "request for token positions timed out") );
			
			JsonArray retVal = new JsonArray();
			
			for (HashMap.Entry<String,Double> entry : Wallet.reqPositionsMap(walletAddr).entrySet() ) {
				HashMap stock = m_main.getStockByTokAddr( entry.getKey() );

				if (stock != null && entry.getValue() >= m_config.minTokenPosition() ) {
					JsonObject resp = new JsonObject();
					resp.put("conId", stock.get("conid") );
					resp.put("symbol", stock.get("symbol") );
					resp.put("price", getPrice(stock) );
					resp.put("quantity", entry.getValue() ); 
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
			require( Util.isInteger(conidStr), RefCode.INVALID_REQUEST, "%s is not a valid conid", conidStr);
			int conid = Integer.parseInt(conidStr);
			
			Stock stock = m_main.getStock(conid);
			
			boolean inside = m_main.m_tradingHours.insideAnyHours( stock.getBool("is24hour"), null, null);
			stock.put( "exchangeStatus", inside ? "open" : "closed");  // this updates the global object and better be re-entrant
			
			respond(stock);
		});
	}
	
	/** Backend-style msg; conid is last parameter */  // when is this used? pas
	public void handleGetPrice() {
		wrap( () -> {
			String conidStr = Util.getLastToken(m_uri, "/");
			require( Util.isInteger(conidStr), RefCode.INVALID_REQUEST, "%s is not a valid conid", conidStr);
			int conid = Integer.parseInt(conidStr);
			returnPrice(conid, false);
		});
	}
	
	/** Redeem (sell) RUSD */ 
	public void handleRedeem() {
		wrap( () -> {
			String walletAddr = getWalletFromUri();
			
			// cookie comes in the message payload (could easily be changed to Cookie header, just update validateCookie() ) 
			parseMsg();
			validateCookie(walletAddr);

			Rusd rusd = m_config.rusd();
			Busd busd = m_config.busd();

			// not needed, waitForHash() will itself eventually timeout
			//setTimer( m_config.timeout(), () -> timedOut( "redemption request timed out") );

			double rusdPos = rusd.getPosition(walletAddr);  // make sure that rounded amt is not slightly more or less
			require( rusdPos > 0, RefCode.INSUFFICIENT_FUNDS, "No RUSD in user wallet to redeem");
			
			log( LogType.REDEEM, "%s is selling %s RUSD", walletAddr, rusdPos);

			double busdPos = busd.getPosition( Accounts.instance.getAddress("RefWallet") );
			if (busdPos >= rusdPos) {
				rusd.sellRusd(walletAddr, m_config.busd(), rusdPos)
					.waitForHash();
				
				// QUESTION: can we send back a partial response before the hash is ready using chunks or WebSockets?
				
				respondOk();  // wait for completion. pas 
			}
			else {  // we don't use require here because we want to call alert()
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
				respond(m_main.type2Config() );
			}
		});
	}

	/** Return transactions for a specific user or for all users;
	 *  populates the two panels on the Dashboard */
	public void handleReqCryptoTransactions(HttpExchange exch) {
		wrap( () -> {
			parseMsg();
			String wallet = m_map.get("wallet_public_key");
			Main.require( S.isNull(wallet) || Util.isValidAddress(wallet), RefCode.INVALID_REQUEST, "The wallet address is invalid");

			m_main.sqlConnection( conn -> {
				String where = S.isNotNull(wallet) 
						? String.format( "where lower(wallet_public_key)='%s'", wallet.toLowerCase() )
						: "";
				respond(conn.queryToJson("select * from crypto_transactions %s order by timestamp desc limit 20", where) );
			});
		});
	}

	private JsonArray trim(JsonArray json) {
		json.forEach( obj -> {
			((HashMap)obj).remove("created_at");
			((HashMap)obj).remove("updated_at");
		});
		return json;
	}

	public void handleGetUserByWallet() {
		wrap( () -> {
			String walletAddr = getWalletFromUri();
			
			m_main.sqlConnection( conn -> {
				JsonArray ar = conn.queryToJson(
						"select * from users where lower(wallet_public_key) = '%s'", 
						walletAddr.toLowerCase() );
				Main.require( ar.size() == 1, RefCode.INVALID_REQUEST, "Wallet address %s not found", walletAddr);
				
				respond( (JsonObject)trim( ar).get(0) );
			});
		});
	}

	public void handleWalletUpdate() {
		wrap( () -> {
			parseMsg();    // look to see what parameters are being passed; at lease we should update the time  
			respondOk();
		});
	}

	public void handleMyWallet() {
		wrap( () -> {
			String walletAddr = getWalletFromUri();
			
			Wallet wallet = new Wallet(walletAddr);
			
			JsonObject rusd = new JsonObject();
			rusd.put( "name", "RUSD");
			rusd.put( "balance", wallet.getBalance(m_config.rusdAddr() ) );
			rusd.put( "tooltip", m_config.getTooltip(Tooltip.rusdBalance) );
			rusd.put( "buttonTooltip", m_config.getTooltip(Tooltip.redeemButton) );
			
			JsonObject busd = new JsonObject();
			busd.put( "name", "USDC");
			busd.put( "balance", wallet.getBalance( m_config.busdAddr() ) );
			busd.put( "tooltip", m_config.getTooltip(Tooltip.busdBalance) );
			busd.put( "buttonTooltip", m_config.getTooltip(Tooltip.approveButton) );
			busd.put( "approvedBalance", m_config.busd().getAllowance(walletAddr, m_config.rusdAddr() ) );
			busd.put( "stablecoin", true);
			
			JsonObject base = new JsonObject();
			base.put( "name", "MATIC");  // pull from config
			base.put( "balance", MoralisServer.getNativeBalance(walletAddr) );
			base.put( "tooltip", m_config.getTooltip(Tooltip.baseBalance) );
			
			JsonArray ar = new JsonArray();
			ar.add(rusd);
			ar.add(busd);
			ar.add(base);
			
			JsonObject obj = new JsonObject();
			obj.put( "refresh", 2000);
			obj.put( "tokens", ar);
			respond(obj);
		});
	}

	/** This is for use outside the context of the reflection web site */
	public void handleMint() throws IOException { 
		String response;

		try {
			String addr = Util.getLastToken( m_uri, "/");
			require( Util.isValidAddress(addr), RefCode.INVALID_REQUEST, "Correct usage is: .../mint/wallet_address");
			mint( addr);
			response = m_config.mintHtml();
		}
		catch (Exception e) {
			e.printStackTrace();
			response = "An error occurred - " + e.getMessage();
		}
		
		respondWithPlainText(response);
	}
	
	/** Transfer some BUSD and ETH to the user's wallet */
	void mint( String dest) throws Exception {
		Util.require(dest.length() == 42, "The wallet address is invalid");

		out( "Transferring %s BUSD to %s", m_config.mintBusd(), dest);
		String id1 = Transfer.transfer( Fireblocks.testBusd, 1, dest, m_config.mintBusd(), "Transfer BUSD");
		out( "FB id is %s", id1);

		out( "Transferring %s Goerli ETH to %s", m_config.mintEth(), dest);
		String id2 = Transfer.transfer( Fireblocks.platformBase, 1, dest, m_config.mintEth(), "Transfer ETH");
		out( "FB id is %s", id2);

		log( LogType.MINT, "Minted to %s", dest);
	}

	/** Respond with build date/time */
	public void about() {
		wrap( () -> respond( "Built", Util.readResource( Main.class, "version.txt") ) );
	}

	public void handleHotStocks() {
		wrap( () -> {
			respond( m_main.hotStocks() );
		});
	}
	
	public void handleGetProfile() {
		wrap( () -> {
			String walletAddr = getWalletFromUri();
			
			m_main.sqlConnection( conn -> {
				JsonArray ar = conn.queryToJson(
						"select name, address, email, phone, pan_number, aadhaar from users where wallet_public_key = '%s'", 
						walletAddr.toLowerCase() );
				
				JsonObject obj = ar.size() == 0 
						? new JsonObject() 
						: (JsonObject)ar.get(0);
				
				respond(obj);
			});
		});
	}
	
	public void handleUpdateProfile() {
		wrap( () -> {
            Reader reader = new InputStreamReader( m_exchange.getRequestBody() );
            JsonObject profile = (JsonObject)new JSONParser().parse(reader);  // if this returns a String, it means the text has been over-stringified (stringify called twice)
			
			String walletAddr = profile.getLowerString("wallet_public_key");
			require( Util.isValidAddress(walletAddr), RefCode.INVALID_REQUEST, "Wallet address is invalid");
			
			// set wallet to lower case for insert
			profile.put( "wallet_public_key", walletAddr);
			profile.put( "active", true);
			
			m_main.sqlConnection( conn -> conn.insertOrUpdate("users", profile, "wallet_public_key = '%s'", walletAddr) );
			respondOk();
		});
	}

}

				
//				
//				JSONArray ar = conn.queryToJson("select * name, address, email, phone, pan_number from users where wallet = '%s'", address.toLowerCase() );
//				
//				if (ar.size() == )
//				//require( ar.size() == 1, RefCode.UPDATE_PROFILE, "Please update your profile before submitting an order");
//				
//				MyJsonObject obj = 
//				Profile profile = new Profile(ar.getJsonObj(0) );
//			});
//		});
//	}
//
//	public void handleUpdateProfile() {
//	}
//	
//}
// YOU MAY NEED TO REQUIRE SIGN-IN BEFORE ACCEPTING UPDATE-PROFILE
	