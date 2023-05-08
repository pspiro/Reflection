package reflection;

import static reflection.Main.log;
import static reflection.Main.require;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.sun.net.httpserver.HttpExchange;

import fireblocks.Erc20;
import json.MyJsonObject;
import reflection.SiweTransaction.Session;
import tw.google.Auth;
import tw.google.TwMail;
import tw.util.S;
import util.LogType;

/** Base class for all classes which handle http requests */
public abstract class MyTransaction {
	public enum Stablecoin {
		RUSD, USDC
	}

	static double SMALL = .0001; // if difference between order size and fill size is less than this, we consider the order fully filled
	static final String code = "code";
	static final String message = "message";
	public static final String exchangeIsClosed = "The exchange is closed. Please try your order again after the stock exchange opens. For US stocks and ETF's, this is usually 4:00 EST (14:30 IST).";
	public static final String etf24 = "ETF-24";  // must match type column from spreadsheet
	static final String ibeos = "IBEOS";  // IB exchange w/ 24 hour trading for ETF's
	
	protected Main m_main;
	protected HttpExchange m_exchange;
	protected boolean m_responded;  // only respond once per transaction
	protected ParamMap m_map = new ParamMap();
	protected String m_uri;

	MyTransaction( Main main, HttpExchange exchange) {
		m_main = main;
		m_exchange = exchange;
		m_uri = Main.getURI(m_exchange);  // all lower case
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
			try {
	            Reader reader = new InputStreamReader( m_exchange.getRequestBody() );

	            JSONObject jsonObject = (JSONObject)new JSONParser().parse(reader);  // if this returns a String, it means the text has been over-stringified (stringify called twice)

	            for (Object key : jsonObject.keySet() ) {
	            	Object value = jsonObject.get(key);
	            	require( key instanceof String, RefCode.INVALID_REQUEST, "Invalid JSON, key is not a string");

	            	if (value != null) {
	            		m_map.put( (String)key, value.toString() );  // tags are mixed case
	            	}
	            }

	            S.out( "  parsed POST request " + jsonObject);
			}
			catch( RefException e) {  // catch the above require() call
				throw e;
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
			return respondFull( Util.toJsonMsg( data), 200, null);
		}
		
		// can't throw an exeption here
		Exception e = new Exception("MyTransaction.respond(Object...) called with wrong number of parameters");
		e.printStackTrace();
		return respondFull( RefException.eToJson(e, RefCode.UNKNOWN), 400, null);
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
		}
		catch (Exception e) {
			e.printStackTrace();
			log( LogType.ERROR, "Exception while responding with json");
		}
		m_responded = true;
		return true;
	}

	/** The main difference between Exception and RefException is that Exception is not expected and will print a stack trace.
	 *  Also Exception returns code UNKNOWN since none is passed with the exception */
	void wrap( ExRunnable runnable) {   // another name for this might be "mustRespond()"
		try {
			runnable.run();
		}
		catch( RefException e) {
			boolean responded = respondFull( 
					e.toJson(), 
					400, 
					null);  // return false if we already responded

			// display log except for timeouts where we have already responded
			if (responded || e.code() != RefCode.TIMED_OUT) {
				log( LogType.ERROR, e.toString() );
			}
		}
		catch( Exception e) {
			e.printStackTrace();
			log( LogType.ERROR, S.notNull( e.getMessage() ) );
			respondFull( 
					RefException.eToJson(e, RefCode.UNKNOWN),
					400,
					null);
		}
	}

	/** Runnable, returns void, throws Exception */
	public interface ExRunnable {
		void run() throws Exception;
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
	protected void alert(String subject, String body) {
		try {
			TwMail mail = Auth.auth().getMail();
			mail.send(
					"RefAPI", 
					"peteraspiro@gmail.com", 
					"peteraspiro@gmail.com",
					subject,
					body,
					"plain");
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}

	
	interface Inside {
		void run(boolean inside) throws Exception;
	}
	
	/** Check if we are inside trading hours. For ETF's, check smart; if that fails,
	 *  check IBEOS and change the exchange on the contract passed in to IBEOS.
	 *  Note that the callback is wrapped */
	void insideAnyHours( Contract contract, Inside runnable) {
		insideHours( contract, inside -> {
			
			// if auto-fill is on, always return true, UNLESS simtime is passed
			// which means this is called by a test script
			if (Main.m_config.autoFill() && m_map.get("simtime") == null) {
				runnable.run(true);
				return;
			}
			
			if (!inside && etf24.equals( m_main.getType( contract.conid() ) ) ) {
				contract.exchange(ibeos);
				insideHours( contract, runnable);
			}
			else {
				runnable.run(inside);
			}
		});
	}

	/** Call true or false on the Inside runnable */
	void insideHours( Contract contract, Inside runnable) {
		m_main.orderController().reqContractDetails(contract, list -> {
			wrap( () -> {
				require( !list.isEmpty(), RefCode.INVALID_REQUEST, "No contract details");

				ContractDetails deets = list.get(0);
				deets.simTime( m_map.getParam("simtime") );  // this is for use by the test scripts in TestOutsideHours only

				runnable.run( inside( deets) );
			});
		});
	}
	

	/** Return true if we are current inside trading hours OR liquid hours.
	 *  When running test scripts, simTime param will be set and used. */
	static boolean inside( ContractDetails deets) throws Exception {
		return inside( deets, deets.tradingHours() ) ||
			   inside( deets, deets.liquidHours() );
	}

	/** Return true if we are inside the specified hours; uses deets.simTime if set.
	 * @throws Exception */
	static boolean inside(ContractDetails deets, String hours) throws Exception {
		return Util.inside( deets.getNow(), deets.conid(), hours, deets.timeZoneId() );
	}

	/** Validate the cookie or throw exception, and update the access time on the cookie.
	 *  They could just send the nonce, it's the only part of the cookie we are using
	 *  @param walletAddr could be null */
	MyJsonObject validateCookie(String walletAddr) throws Exception {
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
	void returnPrice(int conid) throws RefException {
		Prices prices = m_main.getPrices( conid);
		require(prices.hasAnyPrice(), RefCode.NO_PRICES, "No prices available for conid %s", conid);

		S.out( "Returning prices  bid=%s  ask=%s  for conid %s", prices.bid(), prices.ask(), conid);
		respond( prices.toJson(conid) );
	}

	/** Top-level method. */
	protected Erc20 stablecoin() throws Exception {
		return m_map.getEnumParam("currency", Stablecoin.values() ) == Stablecoin.USDC
				? Main.m_config.busd() : Main.m_config.rusd();
	}
	
}
