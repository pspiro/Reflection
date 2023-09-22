package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import java.io.IOException;
import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

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
import tw.google.Auth;
import tw.google.TwMail;
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
			// read wallet address into m_walletAddr (last token in URI)
			getWalletFromUri();
			
			// query positions from Moralis
			setTimer( m_config.timeout(), () -> timedOut( "request for token positions timed out") );
			
			JsonArray retVal = new JsonArray();
			
			Util.forEach( Wallet.reqPositionsMap(m_walletAddr).entrySet(), entry -> {
				JsonObject stock = m_main.getStockByTokAddr( entry.getKey() );

				if (stock != null && entry.getValue() >= m_config.minTokenPosition() ) {
					JsonObject resp = new JsonObject();
					resp.put("conId", stock.get("conid") );
					resp.put("symbol", stock.get("symbol") );
					resp.put("price", getPrice(stock) );
					resp.put("quantity", entry.getValue() ); 
					retVal.add(resp);   // alternatively, you could just add the whole stock to the array, but you would need to adjust the column names in the Monitor
				}
			});
			
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
			// read wallet address into m_walletAddr (last token in URI)
			getWalletFromUri();
						
			require( m_config.allowRedemptions(), RefCode.REDEMPTIONS_HALTED, "Redemptions are temporarily halted. Please try again in a little while.");
			require( m_main.validWallet( m_walletAddr, "Sell"), RefCode.ACCESS_DENIED, "Your redemption cannot be processed at this time (L6)");  // make sure wallet is not blacklisted

			// cookie comes in the message payload (could easily be changed to Cookie header, just update validateCookie() ) 
			parseMsg();
			validateCookie(m_walletAddr);
			
			Rusd rusd = m_config.rusd();
			Busd busd = m_config.busd();

			double rusdPos = Util.truncate( rusd.getPosition(m_walletAddr), 4); // truncate after four digits because Erc20 rounds to four digits when converting to Blockchain mode
			require( rusdPos > .004, RefCode.INSUFFICIENT_FUNDS, "No RUSD in user wallet to redeem");
	
			double busdPos = busd.getPosition( Accounts.instance.getAddress("RefWallet") );
			if (busdPos >= rusdPos) {  // we don't have to worry about decimals here, it shouldn't come down to the last penny
				olog( LogType.REDEEM, "amt", rusdPos);

				rusd.sellRusd(m_walletAddr, busd, rusdPos)  // rounds to 4 decimals, but RUSD can take 6
					//.waitForHash();
					.waitForStatus("COMPLETED");
				
				respondOk();  // wait for completion. pas

				report( m_walletAddr, busd, rusdPos, true); // informational only, don't throw an exception
			}
			else {  // we don't use require here because we want to call alert()
				
				// check for previous unfilled request 
				require( Main.m_config.sqlQuery( conn -> conn.queryToJson( "select * from redemptions where wallet_public_key = '%s' and fulfilled = false", m_walletAddr.toLowerCase()) ).isEmpty(), 
						RefCode.REDEMPTION_PENDING, 
						"There is already an outstanding redemption request for this wallet; we appreciate your patience.");

				// write unfilled report to DB
				report( m_walletAddr, busd, rusdPos, false);
				
				// send alert email so we can move funds from brokerage to wallet
				String str = String.format( 
						"Insufficient stablecoin in RefWallet for RUSD redemption  \nwallet=%s  requested=%s  have=%s  need=%s",
						m_walletAddr, rusdPos, busdPos, (rusdPos - busdPos) );
				alert( "MOVE FUNDS NOW TO REDEEM RUSD", str);
				
				// report error back to user
				throw new RefException( RefCode.INSUFFICIENT_FUNDS, str);
			}
		});
	}
	
	private static void report(String walletAddr, Busd busd, double rusdPos, boolean fulfilled) {
		Util.wrap( () -> {
			JsonObject obj = new JsonObject();
			obj.put( "wallet_public_key", walletAddr.toLowerCase() );
			obj.put( "stablecoin", busd.getName() );
			obj.put( "amount", rusdPos);
			obj.put( "fulfilled", fulfilled);
	
			Main.m_config.sqlCommand( conn -> conn.insertJson("redemptions", obj) );
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
			if (S.isNotNull(key) ) {
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
				respond(conn.queryToJson("select * from crypto_transactions %s order by created_at desc limit 20", where) );
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
			// read wallet address into m_walletAddr (last token in URI)
			getWalletFromUri();
			
			m_main.sqlConnection( conn -> {
				JsonArray ar = conn.queryToJson(
						"select * from users where lower(wallet_public_key) = '%s'", 
						m_walletAddr.toLowerCase() );
				Main.require( ar.size() == 1, RefCode.INVALID_REQUEST, "Wallet address %s not found", m_walletAddr);
				
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
			// read wallet address into m_walletAddr (last token in URI)
			getWalletFromUri();

			Wallet wallet = new Wallet(m_walletAddr);
			
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
			busd.put( "approvedBalance", m_config.busd().getAllowance(m_walletAddr, m_config.rusdAddr() ) );
			busd.put( "stablecoin", true);
			
			JsonObject base = new JsonObject();
			base.put( "name", "MATIC");  // pull from config
			base.put( "balance", MoralisServer.getNativeBalance(m_walletAddr) );
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

		out( "Minted to %s", dest);
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
			// read wallet address into m_walletAddr (last token in URI)
			getWalletFromUri();
			
			m_main.sqlConnection( conn -> {
				JsonArray ar = conn.queryToJson(
						"select first_name, last_name, address, email, phone, pan_number, aadhaar from users where wallet_public_key = '%s'", 
						m_walletAddr.toLowerCase() );
				
				JsonObject obj = ar.size() == 0 
						? new JsonObject() 
						: (JsonObject)ar.get(0);
				
				respond(obj);
			});
		});
	}

	static HashMap<String,String> mapWalletToCode = new HashMap<>();
	
	public void validateEmail() {
		wrap( () -> {
            JsonObject data = parseToObject();
			
            String wallet = data.getLowerString( "wallet_public_key");
			require( Util.isValidAddress(wallet), RefCode.INVALID_REQUEST, "The wallet '%s' is invalid", wallet);

			String email = data.getString( "email");
			require( Util.isValidEmail(email), RefCode.INVALID_REQUEST, "The email '%s' is invalid for wallet '%s'", email, wallet);
			
			String code = Util.uin(5);
			S.out( "Emailing verification code '%s' for wallet '%s' to email '%s'", code, wallet, email);
			
			mapWalletToCode.put( wallet, code); // save code 
			
			TwMail mail = Auth.auth().getMail();
			mail.send(
					"Reflection", 
					"peteraspiro@gmail.com", 
					email,
					"Reflection Verification Code",
					"Your Reflection Verification code is: " + code,
					"plain");
			
			respondOk();
		});
	}

	public void handleUpdateProfile() {
		wrap( () -> {
            Profile profile = new Profile( parseToObject() );
			
			profile.validate();
			
			String wallet = profile.wallet();
			
			// if email has changed, they must submit a valid verification code from the validateEmail() message
			if (!profile.email().equalsIgnoreCase( getExistingEmail(wallet) ) ) {
				require( profile.getString("email_confirmation").equalsIgnoreCase(mapWalletToCode.get(wallet) ),
						RefCode.INVALID_REQUEST,
						"The email verification code is incorrect");
				mapWalletToCode.remove(wallet); // remove only if there is a match so they can try again
			}

			// add/remove fields to prepare for database insertion
			profile.remove("email_confirmation"); // don't want to store this in db
			
			// insert or update record in users table
			m_main.sqlConnection( conn -> conn.insertOrUpdate("users", profile, "wallet_public_key = '%s'", wallet) );
			respondOk();
		});
	}

	private String getExistingEmail(String walletAddr) throws Exception {
		JsonArray res = Main.m_config.sqlQuery( conn -> conn.queryToJson("select email from users where wallet_public_key = '%s'", walletAddr) );
		return res.size() > 0
				? res.get(0).getString("email")
				: "";
	}

	public void handleSignup() {
		wrap( () -> {
			JsonObject obj = parseToObject();
			S.out( "Received " + obj);
			obj.update( "wallet_public_key", val -> val.toString().toLowerCase() );
			m_config.sqlCommand( conn -> conn.insertJson("signup", obj) );
			respondOk();
		});
	}

}
