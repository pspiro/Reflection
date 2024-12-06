package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import chain.Chain;
import chain.Stocks.Stock;
import common.Alerts;
import common.SignupReport;
import common.Util;
import http.MyClient;
import onramp.Onramp;
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

	/** Handle a Backend-style event. Conid is last parameter
	 * called from trade page in prod, not used in paper, can be removed after upgrade
	 * 
	 * @return 
		{
		// "smartcontractid": "0xd3383F039bef69A65F8919e50d34c7FC9e913e20",
		// could add tradingView if desired
		"conid": "8314",
		"symbol": "IBM",
		"description": "International Business Machines",
		"type": "Stock",
		"bid": 128.5
		"ask": 128.78,
		}
	 */
	public void handleGetStockWithPrice() {
		wrap( () -> {
			Stock stock = m_main.getStock( getConidFromUri() );
			//var token = m_main.getStock( getConidFromUri() );
			// is token addr really needed here? if so, frontend must pass the cookie
			
			Session session = m_main.m_tradingHours.getTradingSession( stock.rec().is24Hour(), null);

			var json = Util.toJson( 
					//"smartcontractid",   // really needed
					//"exchange", stock.rec().exchange(),
					"symbol", stock.symbol(), 
					"bid", stock.prices().anyBid(),
					"ask", stock.prices().anyAsk(),
					"description", stock.rec().description(),
					"conid", String.valueOf( stock.conid() ),
					"type", stock.rec().type(),
					"exchangeStatus", session != Session.None ? "open" : "closed"  // this updates the global object and better be re-entrant
					);
			
			respond(json);
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
	 *  is not so time-dependent.
	 *  
	 *  @return transactions from all blockchains
	 *   */
	public void handleReqCryptoTransactions(HttpExchange exch) {
		wrap( () -> {
			parseMsg();
			m_walletAddr = m_map.getWalletAddress("wallet_public_key");
			
			// NOTE that chainid can be null in the json even though it is an integer;
			// Frontend can handle that

			m_main.queueSql( conn -> {
				wrap( () -> {
					String sql = String.format("""
							select * from transactions 
							where blockchain_hash <> '' and wallet_public_key='%s'
							order by created_at desc
							limit 20""", m_walletAddr.toLowerCase() );
					
					JsonArray ar = conn.queryToJson( sql);
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
	
				double rusdBalance = chain().rusd().getPosition( m_walletAddr);
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
						chain().rusd().mintRusd(m_walletAddr, m_config.autoReward(), chain().getAnyStockToken() );
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

	public void handleMyWallet() {
		wrap( () -> {
			parseMsg();
			getWalletFromUri();
			setChainFromHttp();
			
			String url = String.format( "%s/mywallet/%s",
					chain().params().localHook(),
					m_walletAddr);
			
			JsonObject json = MyClient.getJson( url);
			
			String refCode = json.getString( "code");
			int responseCode = S.isNotNull( refCode) && !refCode.equals( "OK") ? 400 : 200;
			respondFull( json, responseCode, null);
		});
	}

	/** Respond with build date/time */
	public void about() {
		wrap( () -> respond( "Built", Util.readResource( Main.class, "version.txt") ) );
	}

	public void handleHotStocks() {
		wrap( () -> {
			respond( m_main.stocks().hotStocks() );
		});
	}

	public void handleWatchList() {
		wrap( () -> {
			respond( m_main.stocks().watchList() );
		});
	}
	
	public void handleAllStocks() {
		wrap( () -> {
			parseMsg();
			getWalletFromUri();
			setChainFromHttp();
			
			respond( chain().getAllStocks( m_main.stocks() ) );
		});
	}
	
	public void handleSignup() {
		wrap( () -> {
			parseMsg();
			out( m_map.obj() );
			
			String first = m_map.getUnescapedString("first");  // it would have been better just to unesc the whole uri
			String last = m_map.getUnescapedString("last");
			String email = m_map.getUnescapedString("email").toLowerCase();
			String referer = m_map.getUnescapedString("referer");
			
			Util.require( Util.isValidEmail( email), "invalid email in signup message");
						
			//redirect( m_config.baseUrl() );
			respondOk();
			
			// write them all until we get this working
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
				if (sql.insertOrUpdate("signup", obj, "where lower(email) = '%s'", email) ) {
					
					out( "Sending email to %s", email);
					
					String text = Util.readResource( this.getClass(), "signup_email.txt")
							.replaceAll( "\\{name\\}", Util.initialCap( first) );
					
					m_config.sendEmail(email, "Welcome to Reflection", text);
				}
			});

			// if wallet address was included, it means user has connected their wallet
			// so insert record into users table to connect wallet to name;
			// alternatively, we could update signup table with the wallet address
			m_walletAddr = m_map.getString( "wallet_public_key");
			if (S.isNotNull( m_walletAddr) && getUser() == null) {
				out( "Adding to users table: " + obj);
				JsonObject user = new JsonObject();
				user.put( "wallet_public_key", m_walletAddr.toLowerCase() );
				user.put( "email", email);
				user.putIf( "first_name", first);
				user.putIf( "last_name", last);
				m_config.sqlCommand( sql -> sql.insertJson( "users", user) );
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
			var log = parseToObject();
			out( "received log entry " + log);
			
			if (log.getString( "origin").equals( "onboard") ) {
				handleOnboard( log);
			}
			
			respondOk();
		});
	}

	private void handleOnboard(JsonObject log) throws Exception {
		String email = log.getString( "email").toLowerCase();
		Util.require( S.isNotNull( email), "Error: null email when updating signup table");
		
		var rec = m_config.sqlQueryOne( "select * from signup where email = '%s'", email);
		Util.require( rec != null, "Error: no signup record for email " + email);
		
		log.remove( "origin");
		log.remove( "email");
		log.put( "time", Util.yToS.format( new Date() ) );
		
		rec.getOrAddArray( "actions").add( log);
		m_config.sqlCommand( sql -> sql.updateJson("signup", rec, "where email = '%s'", email) );
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
	
	/** return data for the selected stock on the trading page
	 *  uri is: /api/trading-screen-static/wallet/conid 
	 *  
	 *  obsolete, not used anywhere, remove after unified URL rollout */
	public void handleTradingStatic() {
		wrap( () -> {
			String[] ar = m_uri.split("/");
			require( ar.length == 5, RefCode.INVALID_REQUEST, "Wrong number of parameters");
			int conid = Integer.parseInt( ar[4]);
			
			Stock stock = m_main.getStock( conid);
			Util.require( stock != null, "null stock");
			
			var token = chain().getTokenByConid( conid);
			Util.require( token != null, "null token");
			
			JsonObject resp = Util.toJson(
					"smartContractid", token.address(), 
					"symbol", stock.symbol(),
					"tokenSymbol", token.name(),
					"description", stock.rec().description(),
					"conid", stock.conid(),
					"tradingView", stock.rec().tradingView()
					);
			
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
			
			parseMsg();
			setChainFromHttp();
			
			int conid = Integer.parseInt( ar[4]);
			require( conid > 0, RefCode.INVALID_REQUEST, "Invalid conid");
			
			String url = String.format( "%s/get-wallet-map/%s", 
					chain().params().localHook(),
					m_walletAddr.toLowerCase() );

			// query for wallet positions (map style)
			JsonObject json = MyClient.getJson( url);
			JsonObject positions = json.getObject( "positions"); // you could improve this and create a special query just for this
			Util.require(positions != null, "Error: null positions returned from HookServer for wallet %s", m_walletAddr); // this could happen if the HookServer is restarting or not responsive; we could alternatively just return all zeros 
			
			String tokenAddr = chain().getTokenByConid( conid).address();
			
			Prices prices = m_main.getStock( conid).prices();
			Util.require( prices != null, "Error: cannot find prices for conid %s", conid);
			
			// require(prices.hasAnyPrice(), RefCode.NO_PRICES, "No prices available for conid %s", conid);
			// Q what to do if there are no prices
			
			Chain chain = chain();

			respond(			
				"exchangeStatus", "open",
				"exchangeTime", "n/a",
				"stockTokenBalance", positions.getDouble( tokenAddr), 
				"rusdBalance", positions.getDouble( chain.params().rusdAddr() ),
				"nonRusdBalance", positions.getDouble( chain.params().busdAddr() ),
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
			//validateCookiee("checkIdentity");
			
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

						var ar = SignupReport.create( days, sql, m_config.chains().polygon().rusd(), null);
						respondFull( ar, 200, null, "text/html");						
					});
				}); 
			});
		});
	}

	/** This is called as part of on-boarding process */
	public void handleFundWallet() {
		wrap( () -> {
			parseMsg();
			m_walletAddr = m_map.getWalletAddress("wallet_public_key");
			validateCookie("fund-wallet");
			setChainFromHttp();
			
			String first = m_map.getRequiredString( "firstName");
			String last = m_map.getRequiredString( "lastName");
			String email = m_map.getRequiredString( "email");

			double amount = m_map.getRequiredDouble("amount");
			require( amount == 100 || amount == 500, RefCode.INVALID_REQUEST, "The award amount is invalid");

			// insert user profile only if missing
			var user = getUser();
			if (user == null) {
				out( "creating user profile");
				m_config.sqlCommand( sql -> sql.insertJson("users", Util.toJson(
						"wallet_public_key", m_walletAddr,
						"first_name", first,
						"last_name", last,
						"email", email) ) );
			}
			
			// $500 award requires KYC  
			require( amount == 100 || Util.equalsIgnore( user.getString("kyc_status"), "VERIFIED", "completed"),
					RefCode.INVALID_REQUEST,
					"Error: You must verify your identity before collecting collecting this reward");
			
			// get or create existing locked rec
			var locked = user.getObjectNN( "locked");
			
			// wallet has rusd?
			require( chain().rusd().getPosition( m_walletAddr) < 1, 
					RefCode.INVALID_REQUEST, 
					"This wallet already has some RUSD in it; please empty out the wallet and try again"); 
			
			// already collected a prize?
			require( !locked.getBool( "rewarded"), 
					RefCode.INVALID_REQUEST, 
					"This wallet already collected a prize"); 
			
			// no auto-awards?
			if (chain().params().autoReward() < amount) {
				respond( code, RefCode.OK, Message, "Thank you for registering. Your wallet will be funded shortly.");
				Alerts.alert( "RefAPI", "NEEDS FUNDING", "Wallet %s is waiting to be funded " + amount);
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
				chain().rusd().mintRusd(m_walletAddr, amount, chain().getAnyStockToken() )
					.waitForReceipt();
				
				String message = S.format( "$%s RUSD has been minted into your wallet and you are ready for trading!", amount);
				respond( code, RefCode.OK, Message, message);
			});
		});			
	}

	/** The faucet is for giving some free native token for gas to users.
	 *  We store the amount given in the users.locked.faucet.<blockchain name> */
	public void handleShowFaucet() {
		wrap( () -> {
			parseMsg();
			getWalletFromUri();
			setChainFromHttp();
			respond( code, RefCode.OK, "amount", getFaucetAmt() );
		});
	}

	/** return the amount of native token that the user is eligible to receive;
	 *  currently faucet works on PulseChain only; we would have to add a selector
	 *  on Frontend to support other chains */
	private double getFaucetAmt() throws Exception {
		
		// check how much user has already received from faucet
		double received = getorCreateUser()
				.getObjectNN( "locked")
				.getObjectNN( "faucet")
				.getDouble( chain().params().name()
				);
		
		double faucetAmt = chain().params().faucetAmt();
		
		// let them have the full amount if they have not received the full amount AND
		// their wallet has less than the full amount
		return
				received < faucetAmt && 
				chain().node().getNativeBalance( m_walletAddr) < faucetAmt 
					? faucetAmt 
					: 0;
	}

	/** The faucet is for giving some free native token for gas to users.
	 *  We store the amount given in the users.locked.faucet.<blockchain name> */
	public void handleTurnFaucet() {
		wrap( () -> {
			parseMsg();
			m_walletAddr = m_map.getWalletAddress("wallet_public_key");
			setChainFromHttp();
			
			// require user profile
			require(new Profile( getorCreateUser() ).isValid(), 
					RefCode.INVALID_USER_PROFILE, 
					"Please update your user profile and try again");
			
			double amount = getFaucetAmt();
			Util.require( amount > 0, "This account is not eligible for more native token");
			
			chain().blocks().transfer( chain().params().admin1Key(), m_walletAddr, amount)
				.waitForReceipt();

			// update the faucet object in the user/locked json
			var locked = getorCreateUser().getObjectNN( "locked");  // reads the database again, not so efficient
			locked.getOrAddObject( "faucet").put( chain().params().name(), amount);

			// update the database with new  
			m_config.sqlCommand( sql -> sql.updateJson( 
					"users", 
					Util.toJson( "locked", locked), 
					"wallet_public_key = '%s'", 
					m_walletAddr.toLowerCase() ) );
			
			respond( code, RefCode.OK, Message, "Your wallet has been funded!");
		});
	}
	
}
