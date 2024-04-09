package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import java.text.ParseException;
import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import http.MyClient;
import onramp.Onramp;
import positions.Wallet;
import reflection.Config.Tooltip;
import reflection.TradingHours.Session;
import tw.util.S;

/** This class handles events from the Frontend, simulating the Backend */
public class BackendTransaction extends MyTransaction {

	public BackendTransaction(Main main, HttpExchange exch) {
		super(main, exch);
	}

	public BackendTransaction(Main main, HttpExchange exch, boolean debug) {
		super(main, exch, debug);
	}
	
	/** Used by the My Reflection (portfolio) section on the dashboard
	 *  We're returning the token positions from the blockchain, not IB positions;
	 *  This is obsolete and should be removed, and replaced with handleReqPositionsNew() */
	public void handleReqPositions() {
		wrap( () -> {
			// read wallet address into m_walletAddr (last token in URI)
			getWalletFromUri();
			
			// query positions from Moralis
			setTimer( m_config.timeout(), () -> timedOut( "request for token positions timed out") );
			
			JsonArray retVal = new JsonArray();
			
			Wallet wallet = new Wallet(m_walletAddr);
			
			Util.forEach( wallet.reqPositionsMap(m_main.stocks().getAllContractsAddresses() ).entrySet(), entry -> {
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
			
			retVal.sortJson( "symbol", true);
			respond(retVal);
		});
	}

	/** You'll see exceptions here when the HookServer is restarting */
	public void handleReqPositionsNew() {
		wrap( () -> {
			// read wallet address into m_walletAddr (last token in URI)
			getWalletFromUri();

			String url = String.format( "http://localhost:%s/hook/get-wallet/%s", Main.m_config.hookServerPort(), m_walletAddr.toLowerCase() );

			JsonArray retVal = new JsonArray();

			for (JsonObject pos : MyClient.getJson( url).getArray( "positions") ) {   // returns the following keys: native, approved, positions, wallet
				double position = pos.getDouble("position");
				
				JsonObject stock = m_main.getStockByTokAddr( pos.getString("address") );
				if (stock != null && position >= m_config.minTokenPosition() ) {
					JsonObject resp = new JsonObject();
					resp.put("conId", stock.get("conid") );
					resp.put("symbol", stock.get("symbol") );
					resp.put("price", getPrice(stock) );
					resp.put("quantity", position); 
					retVal.add(resp);
				}
			}
			
			retVal.sortJson( "symbol", true);
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
			Stock stock = m_main.getStock( getConidFromUri() );
			
			Session session = m_main.m_tradingHours.insideAnyHours( stock.is24Hour(), null);
			stock.put( "exchangeStatus", session != Session.None ? "open" : "closed");  // this updates the global object and better be re-entrant
			
			respond(stock);
		});
	}
	
	/** Conid is last parameter of URI 
	 *  Called by Frontend */
	public void handleGetPrice() {
		wrap( () -> {
			returnPrice( getConidFromUri(), false);
		});
	}
	
	/** Redeem (sell) RUSD */ 
	public void handleRedeem() {
		wrap( () -> {
			respond(code, RefCode.OK, "message", "Redeem is temporarily disabled");  // respond OK as long as there was no exception
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
				// this query comes with only two keys:
				// whitepaper_link and tds_tooltip_text
				respond( key, m_main.type2Config().getString(key) );
			}
			else {
				// we get this a lot; why?
				// with no key, we just want the four social media links
				respond(m_main.type2Config() );
			}
		});
	}

	/** Return transactions for a specific user or for all users;
	 *  populates the two panels on the Dashboard. We put this in the queue
	 *  to release the query thread quickly and because this query
	 *  is not so time-dependent */
	public void handleReqCryptoTransactions(HttpExchange exch) {
		wrap( () -> {
			parseMsg();
			String wallet = m_map.get("wallet_public_key");
			Main.require( S.isNull(wallet) || Util.isValidAddress(wallet), RefCode.INVALID_REQUEST, "The wallet address is invalid");

			m_main.queueSql( conn -> {
				wrap( () -> {
					String where = "where blockchain_hash <> ''";  // use blockchain hash because the link is clickable for the user
					if (S.isNotNull(wallet) ) {
						where += String.format(" and lower(wallet_public_key)='%s'", wallet.toLowerCase() );
					}
					JsonArray ar = conn.queryToJson("select * from transactions %s order by created_at desc limit 20", where);
					for (JsonObject obj : ar) fix(obj);  // switch "created_at" to "timestamp"
					respond(ar);
				});
			});
		});
	}
	
	private void fix(JsonObject obj) throws ParseException {
		obj.put( "timestamp", Util.yToS.parse(obj.getString("created_at")).getTime() / 1000 );
		obj.remove("created_at");
		obj.remove("perm_id");
		obj.remove("city");
	}

	/** obsolete, */
	public void handleWalletUpdate() {
		wrap( () -> {
			parseMsg();
			m_walletAddr = m_map.getWalletAddress("wallet_public_key");

			// look to see what parameters are being passed; at least we should update the time
			out( "received wallet-update message with params " + m_map);
			respondOk();
		});
	}

	/** Handle the Persona KYC info */
	public void handleRegister() {
		wrap( () -> {
			parseMsg();

			m_walletAddr = m_map.getWalletAddress("wallet_public_key");

			validateCookie("register");
			
			require( S.isNotNull( m_map.get("kyc_status") ), RefCode.INVALID_REQUEST, "null kyc_status");
			require( S.isNotNull( m_map.get("persona_response") ), RefCode.INVALID_REQUEST, "null persona_response");

			// create record
			JsonObject obj = new JsonObject();
			obj.put( "wallet_public_key", m_walletAddr.toLowerCase() );
			obj.copyFrom( m_map.obj(), "kyc_status", "persona_response");

			// insert or update record in users table with KYC info
			Main.m_config.sqlCommand(sql -> 
				sql.insertOrUpdate("users", obj, "wallet_public_key = '%s'", m_walletAddr.toLowerCase() ) );

			respondOk();
			
			alert("KYC COMPLETED", m_walletAddr);
		});
	}

	public void handleMyWallet() {
		wrap( () -> {
			// read wallet address into m_walletAddr (last token in URI)
			getWalletFromUri();

			Wallet wallet = new Wallet(m_walletAddr);
			
			HashMap<String, Double> map = wallet.reqPositionsMap( 
					m_config.rusdAddr(), 
					m_config.busd().address() ); 
			
			JsonObject rusd = new JsonObject();
			rusd.put( "name", "RUSD");
			rusd.put( "balance", Wallet.getBalance(map, m_config.rusdAddr() ) );
			rusd.put( "tooltip", m_config.getTooltip(Tooltip.rusdBalance) );
			rusd.put( "buttonTooltip", m_config.getTooltip(Tooltip.redeemButton) );
			
			// add info about outstanding RUSD redemption request, if any
			RedeemTransaction trans = liveRedemptions.get(m_walletAddr.toLowerCase() );
			if (trans != null) {
				if (trans.progress() == 100) {
					rusd.put( "text", trans.text() );  // success or error message
					rusd.put( "status", trans.status() );  // Completed or Failed 
					liveRedemptions.remove( m_walletAddr.toLowerCase() );
				}
				else {
					rusd.put( "progress", trans.progress() );
				}
			}
			
			// fix a display issue where some users approved a huge size by mistake
			double approved = Math.min(1000000,m_config.busd().getAllowance(m_walletAddr, m_config.rusdAddr() ));
			
			JsonObject busd = new JsonObject();
			busd.put( "name", m_config.busd().name() );
			busd.put( "balance", Wallet.getBalance( map, m_config.busd().address() ) );
			busd.put( "tooltip", m_config.getTooltip(Tooltip.busdBalance) );
			busd.put( "buttonTooltip", m_config.getTooltip(Tooltip.approveButton) );
			busd.put( "approvedBalance", approved);
			busd.put( "stablecoin", true);
			
			JsonObject base = new JsonObject();
			base.put( "name", "MATIC");  // pull from config
			base.put( "balance", wallet.getNativeBalance() );
			base.put( "tooltip", m_config.getTooltip(Tooltip.baseBalance) );
			
			JsonArray ar = new JsonArray();
			ar.add(rusd);
			ar.add(busd);
			ar.add(base);
			
			JsonObject obj = new JsonObject();
			obj.put( "refresh", m_config.myWalletRefresh() );
			obj.put( "tokens", ar);
			respond(obj);
		});
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
	
	public void handleSignup() {
		wrap( () -> {
			parseMsg();
			out( m_map.obj() );

			redirect( m_config.baseUrl() );
			
			String first = m_map.getUnescapedString("first");  // it would have been better just to unesc the whole uri
			String last = m_map.getUnescapedString("last");
			String email = m_map.getUnescapedString("email");
			String referer = m_map.getUnescapedString("referer");  
			
			// write them all until we get this working
//			if (Util.isValidEmail( email) ) {
				// add entry to signup table
				JsonObject obj = new JsonObject();
				obj.put( "first", first);
				obj.put( "last", last);
				obj.put( "email", email);
				obj.put( "referer", referer);
				obj.put( "country", getCountryCode() );
				obj.put( "ip", Util.left( getFirstHeader( "X-Real-IP"), 15) );
			
				out( "Adding to signup table: " + obj.toString() );
				m_main.queueSql( sql -> sql.insertJson("signup", obj) );
//			}
		});
	}

	public void handleContact() {  // obsolete
		wrap( () -> {
			parseMsg();
			
			// redirect client back to signup page
			redirect(m_config.baseUrl() + "/signup");

			String text = String.format( "name: %s<br>email: %s<br>%s",
					m_map.getString("name"), m_map.getString("email"), m_map.getString("msg") );
			
			m_config.sendEmail("info@reflection.trading", "MESSAGE FROM USER", text); 
		});
	}

//	public void handleSignup() {  // obsolete
//		wrap( () -> {
//			JsonObject signup = parseToObject();
//			out( "Received signup " + signup);
//			signup.update( "wallet_public_key", val -> val.toString().toLowerCase().trim() );
//			require( S.isNotNull( signup.getString("name") ), RefCode.INVALID_REQUEST, "Please enter your name"); 
//			require( S.isNotNull( signup.getString("email") ), RefCode.INVALID_REQUEST, "Please enter your email address");
//			// don't validate wallet, we don't care
//			
//			m_config.sqlCommand( conn -> conn.insertJson("signup", signup) );  // bypass the DbQueue so we would see the DB error
//			respondOk();
//		});
//	}

	public void handleLog() {
		wrap( () -> {
			out( "received log entry " + parseToObject() );
			respondOk();
		});
	}

	public void handleStatus() {
		wrap( () -> {
			respond( Util.toJson( 
					code, RefCode.OK,
					"TWS", m_main.orderConnMgr().isConnected(),
					"IB", m_main.orderConnMgr().ibConnection(),
					"started", Main.m_started,
					"built", Util.readResource( Main.class, "version.txt")
					) );
		});
	}

	/** Return PositionTracker data to Monitor; used for debugging only */
	public void handleGetPositionTracker() {
		wrap( () -> {
			respond( OrderTransaction.dumpPositionTracker() );
		});
	}
	
	/** Used by Frontend to determine if we should enable or disable the Wallet Connect button */
	public void allowConnection() {
		wrap( () -> {
			String country = getCountryCode();
			String ip = getHeader("X-Real-IP");
			
			boolean allow =
					S.isNotNull( country) && m_main.isAllowedCountry(country ) ||
					S.isNotNull( ip) && m_main.isAllowedIP(ip);
			
			respond( Util.toJson( 
					"allow", allow,
					"country", country,
					"ip", ip
					) );
		});
	}

	/** Return IP address and country code passed from nginx; you could change it to
	 *  return all headers; note that it returns an array of values for each. */
	public void handleMyIp() {
		wrap( () -> {
			com.sun.net.httpserver.Headers headers = m_exchange.getRequestHeaders();
			
			S.out( headers.get( "X-Country-Code") );
			S.out( headers.get( "X-Real-IP") );
			
			respond( 
					"X-Country-Code", headers.get( "X-Country-Code"),
					"X-Real-IP", headers.get( "X-Real-IP") );
		});
	}
	
	public void handleTradingStatic() {
		wrap( () -> {
			Stock stock = m_main.getStock( getConidFromUri() );
			
			JsonObject resp = new JsonObject();
			resp.copyFrom( stock, "smartContractid", "symbol", "tokenSymbol", "description", "conid", "tradingView");
			
			respond( resp);
		});
	}

	/** we need both wallet AND conid here */
	public void handleTradingDynamic() {
		wrap( () -> {      // note that the first item in the array is empty string because the uri starts with /
			String[] ar = m_uri.split("/");
			require( ar.length == 5, RefCode.INVALID_REQUEST, "Wrong number of parameters");
			
			m_walletAddr = ar[3].toLowerCase();
			Util.reqValidAddress(m_walletAddr);
			
			int conid = Integer.parseInt( ar[4]);
			
			Stock stock = m_main.getStock( conid);
			
			String url = String.format( "http://localhost:%s/hook/get-wallet-map/%s", 
					Main.m_config.hookServerPort(), 
					m_walletAddr.toLowerCase() );

			// query for wallet positions (map style)
			JsonObject json = MyClient.getJson( url);
			JsonObject positions = json.getObject( "positions"); // you could improve this and create a special query just for this
			
			Prices prices = stock.prices();
			// require(prices.hasAnyPrice(), RefCode.NO_PRICES, "No prices available for conid %s", conid);
			// Q what to do if there are no prices

			respond(			
				"exchangeStatus", "open",
				"exchangeTime", "n/a",
				"stockTokenBalance", positions.getDouble( stock.getSmartContractId() ), 
				"rusdBalance", positions.getDouble( m_config.rusdAddr() ),
				"nonRusdBalance", positions.getDouble( m_config.busd().address() ),
				"nonRusdApprovedAmt", json.getDouble( "approved"),
				"bidPrice", prices.anyBid(),
				"askPrice", prices.anyAsk()
				);
		});
	}

	/** used by Monitor */
	public void handleUserTokenMgr() {
		wrap( () -> respond( UserTokenMgr.getJson() ) );
	}

	public void handleOnramp() {
		wrap( () -> {
			JsonObject obj = parseToObject();
			out( obj);

			m_walletAddr = obj.getString("wallet_public_key");
			Util.reqValidAddress( m_walletAddr);
			
			int orderId = obj.getInt("orderId");
			require( orderId != 0, RefCode.INVALID_REQUEST, "Missing order id");
			
			// don't tie up the http thread
			Util.execute( () -> {
				wrap( () -> {
					int status = Onramp.waitForOrderStatus( orderId, 45); // wait up to 45 sec for order status
					
					if (status == 4 || status == 15) { // success
						respond( code, RefCode.OK, Message, 
								"Your transaction was successful and the crypto should appear in your wallet shortly");
					}
					else if (status < -101) {  // failed invalid order id
						failOnramp( "On-ramping failed: the order id was invalid"); // should never happen
					}
					else if (status < -3) {  // failed KYC
						failOnramp( "On-ramping failed: the names on the bank account and KYC do not match");
					}
					else if (status == -2) { // user aborted
						failOnramp( "On-ramping failed: user aborted the transaction");
					}
					else { // timed out
						failOnramp( String.format( "The transaction made it to status %s but then timed out. It may still complete. Please check your wallet and/or bank account in a little while.", status) );
					}
				});
			});
		});
	}

	private void failOnramp(String message) throws RefException {
		throw new RefException( RefCode.ONRAMP_FAILED, message); 
	}

}
