package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sun.net.httpserver.HttpExchange;

import chain.Chain;
import common.Alerts;
import common.Util;
import common.Util.ExRunnable;
import http.BaseTransaction;
import siwe.SiweTransaction;
import tw.util.S;
import util.LogType;

/** Base class for all classes which handle http requests */
public abstract class MyTransaction extends BaseTransaction {
//	public enum Stablecoin {
//		RUSD, USDT
//	}
	
	static Map<String,Vector<OrderTransaction>> liveOrders = Collections.synchronizedMap( new HashMap<String,Vector<OrderTransaction>>() );  // key is wallet address; used Vector because it is synchronized and we will be adding/removing to the list from different threads; write access to the map should be synchronized 
	static Map<String,RedeemTransaction> liveRedemptions = Collections.synchronizedMap( new HashMap<String,RedeemTransaction>() );  // key is wallet address; just one outstanding Redemption per wallet 
	static Map<String,LiveTransaction> allLiveTransactions = Collections.synchronizedMap( new HashMap<String,LiveTransaction>() );  // key is fireblocks id; records are not added here until we submit to Fireblocks, so it is not the complete list

	static final double SMALL = .0001; // if difference between order size and fill size is less than this, we consider the order fully filled
	public static final String exchangeIsClosed = "The exchange is closed. Please try your order again after the stock exchange opens. For US stocks and ETF's, this is usually 4:00 EST (14:30 IST).";
	public static final String etf24 = "ETF-24";  // must match type column from spreadsheet
	protected static final String Message = "message";

	protected Main m_main;
	protected String m_walletAddr;  // must be mixed case or cookie validation will not work
	protected ParamMap m_map = new ParamMap();  // this is a wrapper around JsonObject that adds functionality; could be reassigned
	private Chain m_chain;

	MyTransaction( Main main, HttpExchange exchange) {
		this( main, exchange, true);
	}

	MyTransaction( Main main, HttpExchange exchange, boolean debug) {
		super(exchange, debug);
		m_main = main;
	}

	/** keys are all lower case; you must use this if you want to call validateCookie() */
	void parseMsg() throws Exception {
		require( m_uri.length() < 4000, RefCode.INVALID_REQUEST, "URI is too long");

		if ("GET".equals(m_exchange.getRequestMethod() ) ) {
			// get right side of ? in URL
			String[] parts = m_uri.split("\\?");  // already lower case
			if (parts.length >= 2) {
				// build map of tag/value, expecting tag=value&tag=value
				String[] params = parts[1].split( "&");
				for (String param : params) {
					String[] pair = param.split( "=");
					if (pair.length >= 2) {
						m_map.put( pair[0], pair[1]);
					}
				}
			}
		}

		else {
			/** This code is obsolete; use parseToObject() instead */
			try {
	            Reader reader = new InputStreamReader( m_exchange.getRequestBody() );
	            m_map = new ParamMap( (JsonObject)new JSONParser().parse(reader) );  // if this returns a String, it means the text has been over-stringified (stringify called twice)
	            if (!(this instanceof OrderTransaction) ) {  // order transaction prints its own log
	            	out( "  parsed POST request " + m_map);
	            }
			}
			catch( ParseException e) {   // this exception does not set the exception message text
				throw new RefException( RefCode.INVALID_REQUEST, "Error parsing json - " + e.toString() );
			}
			catch( Exception e) {
				e.printStackTrace(); // should never happen
				throw new RefException( RefCode.INVALID_REQUEST, "Error parsing json - " + e.getMessage() ); // no point returning the message text because
			}
		}
	}

	void setTimer( long ms, ExRunnable runnable) {
		Timer timer = new Timer();
		timer.schedule( new TimerTask() {  // this could be improved to have only one Timer and hence one Thread for all the scheduling. pas
			@Override public void run() {
				wrap( runnable);
				timer.cancel();
			}
		}, ms);
	}

	static void timedOut( String text, Object... params) throws RefException {
		throw new RefException( RefCode.TIMED_OUT, text, params);
	}

	/** don't throw an exception here, it should not disrupt any other process */
	protected static void alert(String subject, String body) {
		if (m_config.isProduction() ) {
			String text = String.format( "blockchain: %s\n\n%s", m_config.moralisPlatform(), body);
			Alerts.alert( "RefAPI", subject, text);
		}
		else {
			S.out( "Alert %s - %s", subject, body);
		}
	}

	/** Validate the cookie or throw exception, and update the access time on the cookie.
	 *  They could just send the nonce, it's the only part of the cookie we are using
	 *  @param caller is a string describing the caller used for error msg only
	 *  @return siwe message, call getSiweMessage() */
	JsonObject validateCookie(String caller) throws Exception {
		require( Util.isValidAddress(m_walletAddr), RefCode.INVALID_REQUEST, "cannot validate cookie without wallet address");
		// we can take cookie from map or header
		// cookie format is <cookiename=cookievalue> where cookiename is <__Host_authToken><wallet_addr><chainid>
		String cookie = m_map.get("cookie");
//		if (cookie == null) {  // we could pull from the cookie header if desired, but then you have to look for the one with the matching address because there could be multiple __Auth cookies
//			cookie = SiweTransaction.findCookie( m_exchange.getRequestHeaders(), "__Host_authToken");
//		}
		require(cookie != null, RefCode.VALIDATION_FAILED, "Null cookie on %s message from %s", caller, m_walletAddr);
		
		var siweMsg = SiweTransaction.validateCookie( cookie, m_walletAddr);

		int chainId = siweMsg.getSiweMessage().getChainId();
		m_chain = m_config.chain( chainId);
		Util.require( m_chain != null, "invalid chain id %s", chainId);
		
		return siweMsg;
	}

	/** return the Chain from the chain id on the cookie;
	 * 	you must call validateCookie() before calling this method */
	public Chain chain() throws Exception {
		Util.require( m_chain != null, "call validateCookie() before chain()");
		return m_chain;
	}

	/** @return e.g. { "bid": 128.5, "ask": 128.78 } */
	void returnPrice(int conid, boolean csv) throws Exception {
		Prices prices = m_main.stocks().getStockByConid(conid).prices();
		require(prices.hasAnyPrice(), RefCode.NO_PRICES, "No prices available for conid %s", conid);

		// this is used by the google sheet to display prices
		if (csv) {
			String data = S.format( "%s,%s", prices.bid(), prices.ask() );
			respondWithPlainText(data);
		}
		// used by frontend for Trading page 
		else {
			//out( "Returning prices  bid=%s  ask=%s  for conid %s", prices.anyBid(), prices.anyAsk(), conid);
			respond( prices.toJson(conid) );
		}
	}

	@Override protected void jlog(LogType type, JsonObject json) {
		m_main.jlog(type, m_uid, S.notNull(m_walletAddr).toLowerCase(), json);
	}

	/** Assumes the wallet address is the last token in the URI
	 *  Read it into the member variable so it is available for log entries */
	public void getWalletFromUri() throws RefException {
		String walletAddr = Util.getLastToken(m_uri, "/");
		require( Util.isValidAddress(walletAddr), RefCode.INVALID_REQUEST, "Wallet address is invalid");
		m_walletAddr = walletAddr;
	}
	
	/** This method implements the interface method from LiveOrderTransaction in the subclasses */
	public String walletAddr() {
		return m_walletAddr;
	}
	
	/** return existing User object or null */
	protected JsonObject getUser() throws Exception {
		JsonArray ar = m_config.sqlQuery( "select * from users where wallet_public_key = '%s'", m_walletAddr.toLowerCase() );
		return ar.size() == 0 ? null : ar.get( 0);
	}

	/** create a User object for this */
	protected JsonObject getorCreateUser() throws Exception {
		var user = getUser();
		return user != null ? user : new JsonObject();
	}
}
