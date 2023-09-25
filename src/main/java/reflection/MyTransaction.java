package reflection;

import static reflection.Main.require;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.json.simple.JSONAware;
import org.json.simple.JsonObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sun.net.httpserver.HttpExchange;

import common.Alerts;
import common.Util;
import common.Util.ExRunnable;
import fireblocks.Erc20;
import test.MyTimer;
import tw.util.S;
import util.LogType;

/** Base class for all classes which handle http requests */
public abstract class MyTransaction {
	public enum Stablecoin {
		RUSD, USDC
	}
	
	static HashMap<String,Vector<OrderTransaction>> liveOrders = new HashMap<>();  // key is wallet address; used Vector because it is synchronized and we will be adding/removing to the list from different threads; write access to the map should be synchronized 
	static HashMap<String,OrderTransaction> allLiveOrders = new HashMap<>();  // key is fireblocks id; records are not added here until we submit to Fireblocks, so it is not the complete list

	static double SMALL = .0001; // if difference between order size and fill size is less than this, we consider the order fully filled
	static final String code = "code";
	//static final String message = "message";
	public static final String exchangeIsClosed = "The exchange is closed. Please try your order again after the stock exchange opens. For US stocks and ETF's, this is usually 4:00 EST (14:30 IST).";
	public static final String etf24 = "ETF-24";  // must match type column from spreadsheet
	protected static final String Message = "message";

	protected Main m_main;
	protected HttpExchange m_exchange;
	protected boolean m_responded;  // only respond once per transaction
	protected ParamMap m_map = new ParamMap();  // this is a wrapper around JsonObject that adds functionality
	protected String m_uri;
	protected MyTimer m_timer = new MyTimer();
	protected String m_uid;
	protected String m_walletAddr;  // mixed case, I think; would be null for most messages, only some use it
	
	MyTransaction( Main main, HttpExchange exchange) {
		m_main = main;
		m_exchange = exchange;
		m_uid = Util.uid(8);
		m_uri = getURI(m_exchange);  // all lower case, prints out the URI
	}
	
	public String uid() {
		return m_uid;
	}

	/** Note this returns URI in all lower case */
	String getURI(HttpExchange exch) {
		String uri = exch.getRequestURI().toString().toLowerCase();
		out( "----- %s -------------------------", uri);
		return uri;
	}

	// you could encapsulate all these methods in MyExchange

	/** keys are all lower case */
	void parseMsg() throws Exception {
		require( m_uri.length() < 4000, RefCode.INVALID_REQUEST, "URI is too long");

		if ("GET".equals(m_exchange.getRequestMethod() ) ) {
			// get right side of ? in URL
			String[] parts = m_uri.split("\\?");  // already lower case
			if (parts.length >= 2) {
				// build map of tag/value, expecting tag=value&tag=value
				String[] params = parts[1].split( "&");
				//map.parseJson( )
				for (String param : params) {
					String[] pair = param.split( "=");
					require( pair.length == 2, RefCode.INVALID_REQUEST, "Tag/value format is incorrect");
					m_map.put( pair[0], pair[1]);
				}
			}
		}

		else {
			/** This code is obsolete; use parseToObject() instead */
			try {
	            Reader reader = new InputStreamReader( m_exchange.getRequestBody() );
	            m_map = new ParamMap( (JsonObject)new JSONParser().parse(reader) );  // if this returns a String, it means the text has been over-stringified (stringify called twice)	            		
	            out( "  parsed POST request " + m_map);
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
	
	public void respondOk() {
		respond( code, RefCode.OK);
	}

	/** @param data is an array of key/value pairs, does not work with objects */
	synchronized boolean respond( Object...data) {     // this is dangerous and error-prone because it could conflict with the version below
		if (data.length > 1 && data.length % 2 == 0) {
			return respondFull( Util.toJson( data), 200, null);
		}
		
		// can't throw an exeption here
		Exception e = new Exception("MyTransaction.respond(Object...) called with wrong number of parameters");
		e.printStackTrace();
		return respondFull( RefException.eToJson(e), 400, null);
	}

	/** Only respond once for each request
	 *  @return true if we responded just now. */
	boolean respond( JSONAware response) {
		return respondFull( response, 200, null);
	}

	/** @param responseCode is 200 or 400 */
	synchronized boolean respondFull( JSONAware response, int responseCode, HashMap<String,String> headers) {
		if (m_responded) {
			return false;
		}
		
		
		// need this? pas
		try (OutputStream outputStream = m_exchange.getResponseBody() ) {
			m_exchange.getResponseHeaders().add( "Content-Type", "application/json");

			// add custom headers, if any  (add URL encoding here?)
			if (headers != null) {
				for (Entry<String, String> header : headers.entrySet() ) {
					m_exchange.getResponseHeaders().add( header.getKey(), header.getValue() );
				}
			}
			
			String data = response.toString();
			m_exchange.sendResponseHeaders( responseCode, data.length() );
			outputStream.write(data.getBytes());

			out( "  completed in %s ms %s", m_timer.time(), Util.left(data, 200) );
		}
		catch (Exception e) {
			e.printStackTrace();
			elog( LogType.RESPOND_ERR, e);
		}
		m_responded = true;
		return true;
	}
	
	synchronized boolean respondWithPlainText( String data) {
		if (m_responded) {
			return false;
		}
		
		// need this? pas
		try (OutputStream outputStream = m_exchange.getResponseBody() ) {
			m_exchange.getResponseHeaders().add( "Content-Type", "text/html");
			m_exchange.sendResponseHeaders( 200, data.length() );
			outputStream.write(data.getBytes());
			out( "  completed in %s ms %s", m_timer.time(), Util.left(data, 200) );
		}
		catch (Exception e) {
			e.printStackTrace();
			elog( LogType.RESPOND_ERR, e);
		}
		m_responded = true;
		return true;
	}
	
	
	/** The main difference between Exception and RefException is that Exception is not expected and will print a stack trace.
	 *  Also Exception returns code UNKNOWN since none is passed with the exception */
	final void wrap( ExRunnable runnable) {
		try {
			runnable.run();
		}
		catch( RefException e) {
			synchronized(this) {      // must synchronize access to m_responded
				// if we haven't responded yet, log the error and respond
				if (!m_responded) {
					elog( LogType.EXCEPTION, e);
					respondFull(e.toJson(), 400, null);
				}
				// display errors that occurred after the response except for timeouts since that is normal
				else if (e.code() != RefCode.TIMED_OUT) {
					elog( LogType.EXCEPTION, e);
				}
			}
		}
		catch( Exception e) {
			e.printStackTrace();
			elog( LogType.EXCEPTION, e);
			respondFull(RefException.eToJson(e, RefCode.UNKNOWN), 400, null);
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
		Alerts.alert( "RefAPI", subject, body);
	}

	/** Validate the cookie or throw exception, and update the access time on the cookie.
	 *  They could just send the nonce, it's the only part of the cookie we are using
	 *  @param walletAddr could be null */
	JsonObject validateCookie(String walletAddr) throws Exception {
		// we can take cookie from map or header
		// cookie format is <cookiename=cookievalue> where cookiename is <__Host_authToken><wallet_addr><chainid>
		String cookie = m_map.get("cookie");
//		if (cookie == null) {  // we could pull from the cookie header if desired, but then you have to look for the one with the matching address because there could be multiple __Auth cookies
//			cookie = SiweTransaction.findCookie( m_exchange.getRequestHeaders(), "__Host_authToken");
//		}
		Main.require(cookie != null, RefCode.VALIDATION_FAILED, "Null cookie on message requring validation");
		
		return SiweTransaction.validateCookie( cookie, walletAddr);
	}

	/** @return e.g. { "bid": 128.5, "ask": 128.78 } */
	void returnPrice(int conid, boolean csv) throws RefException {
		Prices prices = m_main.getStock(conid).prices();
		require(prices.hasAnyPrice(), RefCode.NO_PRICES, "No prices available for conid %s", conid);

		// this is used by the google sheet to display prices
		if (csv) {
			String data = S.format( "%s,%s", prices.bid(), prices.ask() );
			respondWithPlainText(data);
		}
		// no sure what this is used for
		else {
			out( "Returning prices  bid=%s  ask=%s  for conid %s", prices.anyBid(), prices.anyAsk(), conid);
			respond( prices.toJson(conid) );
		}
	}

	/** Top-level method. */
	protected Erc20 stablecoin() throws Exception {
		return m_map.getEnumParam("currency", Stablecoin.values() ) == Stablecoin.USDC
				? Main.m_config.busd() : Main.m_config.rusd();
	}
	
	/** We need this version because some strings have % characters in them */ 
	void out( Object format) {
		S.out( m_uid + " " + format);
	}
	
	void out( String format, Object... params) {
		S.out( m_uid + " " + format, params);
	}

	void olog(LogType type, Object... ar) {
		jlog( type, Util.toJson(ar) );
	}
	
	void elog(LogType type, Exception e) {
		jlog(type, RefException.eToJson(e) );
	}

	void elog(LogType type, RefException e) {
		jlog(type, e.toJson() );
	}

	void jlog(LogType type, JsonObject json) {
		Main.jlog(type, m_uid, m_walletAddr, json);
	}

	/** Assumes the wallet address is the last token in the URI
	 *  Read it into the member variable so it is available for log entries */
	public void getWalletFromUri() throws RefException {
		m_walletAddr = Util.getLastToken(m_uri, "/");
		require( Util.isValidAddress(m_walletAddr), RefCode.INVALID_REQUEST, "Wallet address is invalid");
	}
	
	/** Parse a POST message and return JsonObject */
	JsonObject parseToObject() throws Exception {
        return (JsonObject)new JSONParser().parse(new InputStreamReader( m_exchange.getRequestBody() ));  // if this returns a String, it means the text has been over-stringified (stringify called twice)
	}
	
	
}
