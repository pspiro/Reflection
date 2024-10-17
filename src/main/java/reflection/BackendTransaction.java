package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import java.text.ParseException;
import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.SignupReport;
import common.Util;
import http.MyClient;
import onramp.Onramp;
import reflection.Config.Tooltip;
import reflection.TradingHours.Session;
import tw.util.S;
import web3.NodeServer;

/** This class handles events from the Frontend, simulating the Backend */
public class BackendTransaction extends MyTransaction {

	public BackendTransaction(Main main, HttpExchange exch) {
		super(main, exch);
	}

	public BackendTransaction(Main main, HttpExchange exch, boolean debug) {
		super(main, exch, debug);
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
			
			Session session = m_main.m_tradingHours.getTradingSession( stock.is24Hour(), null);
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
			
			// we don't look at kyc_status anymore; Frontend should stop sending it
			//require( S.isNotNull( m_map.get("kyc_status") ), RefCode.INVALID_REQUEST, "null kyc_status");
			
			String personaStr = m_map.getString("persona_response");
			require( S.isNotNull( personaStr), RefCode.INVALID_REQUEST, "null persona_response");
			require( JsonObject.isObject(personaStr), RefCode.INVALID_REQUEST, "persona_response is not a valid json object");
			
			JsonObject persona = JsonObject.parse( personaStr);
			String status = persona.getString( "status");
			
			// insert or update record in users table with KYC info
			JsonObject userRec = new JsonObject();
			userRec.put( "wallet_public_key", m_walletAddr.toLowerCase() );
			userRec.put( "kyc_status", status);  // this is the exact "status" text from the json returned by Persona; used to be VERIFIED
			userRec.put( "persona_response", personaStr);
			m_config.sqlCommand(sql -> 
				sql.insertOrUpdate("users", userRec, "wallet_public_key = '%s'", m_walletAddr.toLowerCase() ) );

			// BAIL OUT HERE if they failed the kyc
			// this is unconventional in that we return 400 even though we updated the database
			require( status.equals( "completed"),
					RefCode.INVALID_REQUEST, "KYC failed with status '%s'", status);
			
			String message = "";
			double autoRewarded = 0;
			
			// this is turned off for now; there was a case where a wallet got double-funded
			// in < one second; how is that possible since we create a database entry?
			
			// auto-reward the user?
			if (m_config.autoReward() > 0) {
				// get existing locked rec, or create
				var locked = getorCreateUser().getObjectNN( "locked");
	
				double rusdBalance = m_config.rusd().getPosition( m_walletAddr);
				out( "  alreadyRewarded=%s  rusdBalance=%s", locked.getBool( "rewarded"), rusdBalance);
				
				// check for not rewarded and zero RUSD balance
				if (!locked.getBool( "rewarded") && rusdBalance == 0) { // sends a query
					// set rewarded to true in the db
					locked.put( "rewarded", true);
					
					// update users table with locked
					JsonObject lockRec = Util.toJson(
							"wallet_public_key", m_walletAddr.toLowerCase(),
							"locked", locked); 
					m_config.sqlCommand(sql -> 
						sql.updateJson("users", lockRec, "wallet_public_key = '%s'", m_walletAddr.toLowerCase() ) );
					
					// mint $500 for the user
					out( "Minting $%s RUSD reward for %s", m_config.autoReward(), m_walletAddr);
					message = S.format( "$%s RUSD is being minted into your wallet and will appear shortly", (int)m_config.autoReward() );
					autoRewarded = m_config.autoReward();
					Util.executeAndWrap( () -> {
						m_config.rusd().mintRusd(m_walletAddr, m_config.autoReward(), m_main.stocks().getAnyStockToken() );
					});
				}
				else {
					out( "WARNING: user KYC'ed but not receiving reward, check locked->rewarded and RUSD balance for wallet %s", m_walletAddr);  
				}
			}			

			respond( code, RefCode.OK, Message, message);
			alert("KYC COMPLETED", String.format( "wallet=%s  autoReward=%s", m_walletAddr, autoRewarded) );
		});
	}

	/** obsolete; myWallet requests are sent directly to HookServer */
	public void handleMyWallet() {
		wrap( () -> {
			out( "warning: mywallet requests should route to HookServer");
			
			// read wallet address into m_walletAddr (last token in URI)
			getWalletFromUri();

			double rusdBal = m_config.rusd().getPosition( m_walletAddr);
			double busdBal = m_config.rusd().getPosition( m_walletAddr);
			
			JsonObject rusd = new JsonObject();
			rusd.put( "name", "RUSD");
			rusd.put( "balance", rusdBal);
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
			busd.put( "balance", busdBal);
			busd.put( "tooltip", m_config.getTooltip(Tooltip.busdBalance) );
			busd.put( "buttonTooltip", m_config.getTooltip(Tooltip.approveButton) );
			busd.put( "approvedBalance", approved);
			busd.put( "stablecoin", true);
			
			JsonObject base = new JsonObject();
			base.put( "name", "MATIC");  // pull from config
			base.put( "balance", NodeServer.getNativeBalance( m_walletAddr) );
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
			if (Util.isValidEmail( email) ) {
				JsonObject obj = new JsonObject();
				obj.put( "email", email);
				obj.putIf( "first", first);
				obj.putIf( "last", last);
				obj.putIf( "referer", Util.urlFromUri( referer) );
				obj.putIf( "country", getCountryCode() );
				obj.putIf( "ip", getUserIpAddress() );
				obj.putIf( "utm_source", getUtmVal("utm_source") );
				obj.putIf( "utm_medium", getUtmVal("utm_medium") );
				obj.putIf( "utm_campaign", getUtmVal("utm_campaign") );
				obj.putIf( "utm_term", getUtmVal("utm_term") );
				obj.putIf( "utm_content", getUtmVal("utm_content") );
				obj.putIf( "user_agent", getUtmVal("user_agent") );  // contains device type, OS, etc
			
				out( "Adding to signup table: " + obj.toString() );
				m_main.queueSql( sql -> {
					if (sql.insertOrUpdate("signup", obj, "where lower(email) = '%s'", email.toLowerCase() ) ) {
						
						out( "Sending email to %s", email);
						
						String text = Util.readResource( this.getClass(), "signup_email.txt")
								.replaceAll( "\\{name\\}", Util.initialCap( first) );
						
						m_config.sendEmail(email, "Welcome to Reflection", text);
					}
				});
			}
		});
	}

	/** frontend might pass "null" */
	private String getUtmVal(String tag) {
		String val = m_map.getUnescapedString( tag);
		return "null".equals( val) ? "" : val;
	}

	/** Called when the user sends a message from the signup page */
	public void handleContact() {
		wrap( () -> {
			parseMsg();
			
			// redirect client back to signup page
			redirect(m_config.baseUrl() + "/signup");

			String text = String.format( "name: %s<br>email: %s<br>%s",
					m_map.getString("name"), m_map.getString("email"), m_map.getString("msg") );
			
			text = Util.unescHtml(text, true); 
			
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
	
	/** Return IP address and country code passed from nginx; you could change it to
	 *  return all headers; note that it returns an array of values for each. */
	public void handleMyIp() {
		wrap( () -> {
			S.out( "countr=%s  ip=%s", getCountryCode(), getUserIpAddress() );
			
			respond( 
					"X-Country-Code", getCountryCode(),
					"X-Real-IP", getUserIpAddress() );
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
					m_config.hookServerPort(), 
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
				"bidPrice", prices.anyBid() * (1. - m_config.sellSpread() ),
				"askPrice", prices.anyAsk() * (1. + m_config.buySpread() )
				);
		});
	}

	/** used by Monitor */
	public void handleUserTokenMgr() {
		wrap( () -> respond( UserTokenMgr.getJson() ) );
	}

	/** used by Monitor */
	public void resetUserTokenMgr() {
		wrap( () -> {
			UserTokenMgr.reset();
			respondOk();
		});
	}

	/** this is a request for onramp order status */
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
						alert( "OnRamp succeeded", m_walletAddr);
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
		alert( "OnRamp failed", m_walletAddr + " " + message);
		throw new RefException( RefCode.ONRAMP_FAILED, message); 
	}

	public void checkIdentity() {
		wrap( () -> {
			parseMsg();
			m_walletAddr = m_map.getWalletAddress("wallet_public_key");
			validateCookie("checkIdentity");
			
			JsonArray ar = m_config.sqlQuery("select kyc_status from users where wallet_public_key = '%s'",
					m_walletAddr.toLowerCase() );
			
			String status = ar.size() == 1 ? status = ar.get( 0).getString( "kyc_status") : null;
			boolean verified = Util.equalsIgnore( status, "VERIFIED", "completed");
			
			respond( Util.toJson(
					"verified", verified,
					"message", verified ? "Your identity has already been confirmed" : "Please confirm your identiy") );
		});
	}

	/** Called by anyone who wants to view the signup report as html in a browser */
	public void handleSagHtml() {
		wrap( () -> {
			Util.execute( () -> {  // don't tie up HTTP thread
				wrap( () -> {
					m_config.sqlCommand( sql -> {
						int days = 3;  
						try {  // number of days to look back might be passed as last token in URI
							days = Integer.valueOf( getLastToken() ); 
						}
						catch( Exception e) {}

						var ar = SignupReport.create( days, sql, m_config.rusd(), null);
						respondFull( ar, 200, null, "text/html");						
					});
				}); 
			});
		});
	}

	public void handleFundWallet() {
		wrap( () -> {
			parseMsg();
			m_walletAddr = m_map.getWalletAddress("wallet_public_key");
			validateCookie("fundWallet");
			
			double amount = m_map.getRequiredDouble("amount");
			require( amount == 100 || amount == 500, RefCode.INVALID_REQUEST, "The award amount is invalid");
			
			var user = getUser();
			require( user != null, RefCode.INVALID_REQUEST, "Error: There is no existing user profile for this wallet");
			
			// $500 award requires KYC  
			require( amount == 100 || Util.equalsIgnore( user.getString("kyc_status"), "VERIFIED", "completed"),
					RefCode.INVALID_REQUEST,
					"Error: You must verify your identity before collecting collecting this reward");
			
			// get or create existing locked rec
			var locked = user.getObjectNN( "locked");
			
			// wallet has rusd?
			require( m_config.rusd().getPosition( m_walletAddr) < 1, 
					RefCode.INVALID_REQUEST, 
					"This wallet already has some RUSD in it; please empty out the wallet and try again"); 
			
			// already collected a prize?
			require( !locked.getBool( "rewarded"), 
					RefCode.INVALID_REQUEST, 
					"This wallet already collected a prize"); 
			
			// no auto-awards?
			if (m_config.autoReward() < amount) {
				respond( code, RefCode.OK, Message, "Thank you for registering. Your wallet will be funded shortly.");
				return;
			}
			
			// update users table with locked BEFORE we award the RUSD
			JsonObject lockRec = Util.toJson(
					"wallet_public_key", m_walletAddr.toLowerCase(),
					"locked", locked.append("rewarded", true) ); 
			m_config.sqlCommand(sql -> 
				sql.updateJson("users", lockRec, "wallet_public_key = '%s'", m_walletAddr.toLowerCase() ) );
			
			// mint award for the user
			out( "Minting $%s RUSD reward for %s", amount, m_walletAddr);
			
			// don't tie up the http thread
			Util.executeAndWrap( () -> {
				m_config.rusd().mintRusd(m_walletAddr, m_config.autoReward(), m_main.stocks().getAnyStockToken() )
					.waitForHash();
				
				String message = S.format( "$%s RUSD has been minted into your wallet and you are ready for trading!", amount);
				respond( code, RefCode.OK, Message, message);
			});
		});			
	}

	public void handleShowFaucet() {
		wrap( () -> {
			getWalletFromUri();
			respond( code, RefCode.OK, "amount", getFaucetAmt() );
		});
	}

	private int getFaucetAmt() throws Exception {
		
		// check how much user has already received from faucet
		int received = getorCreateUser()
				.getObjectNN( "locked")
				.getObjectNN( "faucet")
				.getInt( m_config.blockchainName()
				);
		
		// let them have the full amount if they have not received the full amount AND
		// their wallet has less than the full amount
		return 
				received < m_config.faucetAmt() && 
				NodeServer.getNativeBalance( m_walletAddr) < m_config.faucetAmt() 
					? m_config.faucetAmt() 
					: 0;
	}

	public void handleTurnFaucet() {
		wrap( () -> {
			parseMsg();
			m_walletAddr = m_map.getWalletAddress("wallet_public_key");
			validateCookie("turn-faucet");
			
			int amount = getFaucetAmt();
			Util.require( amount > 0, "This account is not eligible for more native token");
			
			m_config.matic().transfer( m_config.admin1Key(), m_walletAddr, amount)
				.waitForHash();
			
			respond( code, RefCode.OK, Message, "Your wallet has been funded!");
		});
	}
	
//	public Object handleShowFaucet() {
//		wrap( () -> {
//			parseMsg();
//			m_walletAddr = m_map.getWalletAddress("wallet_public_key");
//			validateCookie("showFaucet");
//			
//			require( new Profile( getorCreateUser() ).isValid(), RefCode.INVALID_USER_PROFILE, "Please update your user profile before accepting PLS");
//		}
//			
//			
//	}

}
