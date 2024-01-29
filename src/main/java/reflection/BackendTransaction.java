package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import common.Util;
import positions.MoralisServer;
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
	 *  We're returning the token positions from the blockchain, not IB positions */
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
			
			Session session = m_main.m_tradingHours.insideAnyHours( stock.getBool("is24hour"), null);
			stock.put( "exchangeStatus", session != Session.None ? "open" : "closed");  // this updates the global object and better be re-entrant
			
			respond(stock);
		});
	}
	
	/** Backend-style msg; conid is last parameter */  // when is this used? pas
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
	 *  populates the two panels on the Dashboard */
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
			
			JsonObject rusd = new JsonObject();
			rusd.put( "name", "RUSD");
			rusd.put( "balance", wallet.getBalance(m_config.rusdAddr() ) );
			rusd.put( "tooltip", m_config.getTooltip(Tooltip.rusdBalance) );
			rusd.put( "buttonTooltip", m_config.getTooltip(Tooltip.redeemButton) );
			
			// add info about outstanding RUSD redemption request, if any
			RedeemTransaction trans = liveRedemptions.get(m_walletAddr.toLowerCase() );
			if (trans != null) {
				if (trans.progress() == 100) {
					rusd.put( "text", trans.text() );  // success or error message
					rusd.put( "status", trans.status() );  // Completed or Failed 
					liveRedemptions.remove( trans);
				}
				else {
					rusd.put( "progress", trans.progress() );
				}
			}
			
			// fix a display issue where some users approved a huge size by mistake
			double approved = Math.min(1000000,m_config.busd().getAllowance(m_walletAddr, m_config.rusdAddr() ));
			
			JsonObject busd = new JsonObject();
			busd.put( "name", m_config.busd().name() );
			busd.put( "balance", wallet.getBalance( m_config.busd().address() ) );
			busd.put( "tooltip", m_config.getTooltip(Tooltip.busdBalance) );
			busd.put( "buttonTooltip", m_config.getTooltip(Tooltip.approveButton) );
			busd.put( "approvedBalance", approved);
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
	
	public void handleSignup() {  // obsolete
		wrap( () -> {
			JsonObject signup = parseToObject();
			out( "Received signup " + signup);
			signup.update( "wallet_public_key", val -> val.toString().toLowerCase().trim() );
			require( S.isNotNull( signup.getString("name") ), RefCode.INVALID_REQUEST, "Please enter your name"); 
			require( S.isNotNull( signup.getString("email") ), RefCode.INVALID_REQUEST, "Please enter your email address");
			// don't validate wallet, we don't care
			
			m_config.sqlCommand( conn -> conn.insertJson("signup", signup) );  // bypass the DbQueue so we would see the DB error
			respondOk();
		});
	}

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
	
	/** Top-level method handler */
	public void allowConnection() {
		wrap( () -> {
			String country = getHeader("X-Country-Code");
			String ip = getHeader("X-Real-IP");
			
			boolean allow =
					m_main.isAllowedCountry(country ) ||
					m_main.isAllowedIP(ip);
			
			respond( Util.toJson( 
					"allow", allow,
					"country", country,
					"ip", ip
					) );
		});
	}

}
