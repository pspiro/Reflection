package http;

import static reflection.Main.require;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.json.simple.JSONAware;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import common.LogType;
import common.MyTimer;
import common.Util;
import common.Util.ExRunnable;
import reflection.RefCode;
import reflection.RefException;
import tw.util.S;

/** Base class for all classes which handle http requests */
public class BaseTransaction {
	public static final String code = "code";
	//static final String message = "message";
	protected static boolean m_debug; // if true, will print all messages to log file; also used by each application to print more debug to log

	protected final HttpExchange m_exchange;
	protected boolean m_responded;  // only respond once per transaction
	protected final String m_uri;
	protected final MyTimer m_timer;  // if debug or verbose=true, we print to log when msg is received and when we respond
	protected String m_uid;  // unique for each msg; for live order messages, gets switched to the uid of the order

	public BaseTransaction(HttpExchange exchange, boolean debug) {
		m_exchange = exchange;
		m_uid = Util.uid(8);
		m_timer = debug || m_debug ? new MyTimer() : null;
		m_uri = m_exchange.getRequestURI().toString().toLowerCase();
		
		if (m_timer != null) {
			m_timer.next( "%s ----- %s -------------------------", m_uid, m_uri);
		}
	}
	
	public String uid() {
		return m_uid;
	}

	public void respondOk() {
		respond( code, RefCode.OK);
	}

	/** @param data is an array of key/value pairs, does not work with objects */  // should be made protected. pas
	public synchronized boolean respond( Object...data) {     // this is dangerous and error-prone because it could conflict with the version below
		if (data.length > 1 && data.length % 2 == 0) {
			return respondFull( Util.toJson( data), 200, null);
		}
		
		// can't throw an exeption here
		Exception e = new Exception("BaseTransaction.respond(Object...) called with wrong number of parameters");
		e.printStackTrace();
		return respondFull( RefException.eToJson(e), 400, null);
	}

	/** Only respond once for each request
	 *  @return true if we responded just now. */
	public boolean respond( JSONAware response) {
		return respondFull( response, 200, null);
	}

	public synchronized boolean respondFull( JSONAware response, int responseCode, HashMap<String,String> headers) {
		return respondFull( response, responseCode, headers, "application/json");  // json must be specified here for frontend
	}

	
	/** Respond with json
	 * @param acontentType MUST be set to application/json; it causes axios to parse the payload to an object 
	 * @param responseCode is 200 or 400
	 * @param headers may be null */
	protected synchronized boolean respondFull( JSONAware response, int responseCode, HashMap<String,String> headers, String contentType) {
		if (m_responded) {
			return false;
		}
		
		// need this? pas
		try (OutputStream outputStream = m_exchange.getResponseBody() ) {
			Util.require( S.isNotNull( contentType), "contentType must be set");
			m_exchange.getResponseHeaders().add( "Content-Type", contentType);

			// add custom headers, if any  (add URL encoding here?)
			if (headers != null) {
				for (Entry<String, String> header : headers.entrySet() ) {
					m_exchange.getResponseHeaders().add( header.getKey(), header.getValue() );
				}
			}
			
			String data = contentType.equals( "text/html") ? response.toHtml(true) : response.toString();
			byte[] bytes = data.getBytes();
			m_exchange.sendResponseHeaders( responseCode, bytes.length);
			outputStream.write( bytes);

			if (m_timer != null) {
				// print to standard out
				String output = contentType.equals( "text/html") ? "" : Util.left(data, 500); // don't print html to log file 
				out( "  responded in %s ms %s", m_timer.time(), output);
			}
			else if (responseCode != 200) {
				// if m_timer is null, it means we didn't print out the URI because it's a
				// frequently repeating message; we should print it now since there was an error
				out( m_exchange.getRequestURI().toString().toLowerCase() );
				out( Util.left(data, 200) );
			}				
		}
		catch (Exception e) {
			if ( S.notNull( e.getMessage() ).equals( "Broken pipe") ) {
				S.out( "Error: Broken pipe while responding processing " + m_exchange.getRequestURI() ); // client should wait for a response
				// no need for a stack trace here
			}
			else {
				S.out( e.getMessage() + " while responding processing " + m_exchange.getRequestURI() );
				e.printStackTrace();
			}
		}
		m_responded = true;
		return true;
	}

	/** Used from the signup page */
	protected synchronized boolean redirect( String url) {
		if (m_responded) {
			return false;
		}
		
		// need this? pas
		try {
			m_exchange.getResponseHeaders().add( "Location", url);
			m_exchange.sendResponseHeaders( 302, 0);  // temporary
			
			if (m_timer != null) {
				out( "  responded in %s ms", m_timer.time() );
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			elog( LogType.RESPOND_ERR, e);
		}
		m_responded = true;
		return true;
	}
	
	protected boolean respondWithPlainText( String data) {
		return respondWithPlainText( data, 200);
	}
	
	protected synchronized boolean respondWithPlainText( String data, int code) {
		if (m_responded) {
			return false;
		}
		
		// need this? pas
		try (OutputStream outputStream = m_exchange.getResponseBody() ) {
			m_exchange.getResponseHeaders().add( "Content-Type", "text/html");
			m_exchange.sendResponseHeaders( code, data.length() );
			outputStream.write(data.getBytes());
			
			if (m_timer != null) {
				out( "  responded in %s ms %s", m_timer.time(), Util.left(data, 200) );
			}
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
	public final void wrap( ExRunnable runnable) {
		try {
			runnable.run();
		}
		catch( RefException e) {
			synchronized(this) {      // must synchronize access to m_responded
				// if we haven't responded yet, log the error and respond
				if (!m_responded) {
					elog( LogType.ERROR_1, e);
					respondFull(e.toJson(), 400, null);
				}
				// display errors that occurred after the response except for timeouts since that is normal
				else if (e.code() != RefCode.TIMED_OUT) {
					elog( LogType.ERROR_2, e);
				}
			}
			postWrap( e.code() );
		}
		catch( Throwable e) {
			e.printStackTrace();
			elog( LogType.ERROR_3, e);
			respondFull(RefException.eToJson(e, RefCode.UNKNOWN), 400, null);
			postWrap(null);
		}
	}
	
	/** Overridden in subclass. Called only if there is an exception. */
	protected void postWrap(Enum error) {
	}

	/** We need this version because some strings have % characters in them */ 
	protected void out( Object format) {
		S.out( m_uid + " " + format);
	}
	
	protected void out( String format, Object... params) {
		S.out( m_uid + " " + format, params);
	}

	protected final void olog(LogType type, Object... ar) {
		jlog( type, Util.toJson(ar) );
	}
	
	protected final void elog(LogType type, Throwable e) {
		jlog(type, RefException.eToJson(e) );
	}

	protected final void elog(LogType type, RefException e) {
		jlog(type, e.toJson() );
	}

	/** Overridden in subclass */
	protected void jlog(LogType type, JsonObject json) {
		S.out( "%s LOG %s %s", m_uid, type, json);
	}
	
	/** Parse a POST message and return JsonObject */
	protected JsonObject parseToObject() throws Exception {
		JsonObject ret = JsonObject.parse( m_exchange.getRequestBody() );
		if (m_debug) out( ret);
		return ret;
	}

	public static void setDebug(boolean b) {
		m_debug = b;
	}
	
	public static boolean debug() {
		return m_debug;
	}

	public void handleDebug(boolean v) {
		wrap( () -> {
			m_debug = v;
			respondOk();
		});
	}
	
	/** Assumes the conid is the last token in the URI
	 *  Read it into the member variable so it is available for log entries */
	public int getConidFromUri() throws RefException {
		String conidStr = Util.getLastToken(m_uri, "/");
		require( Util.isInteger(conidStr), RefCode.INVALID_REQUEST, "the conid is invalid", conidStr);
		return Integer.parseInt(conidStr);
	}
	
	/** returns list, never null, could be empty */
	public List<String> getHeaders(String name) {
		Headers headers = m_exchange.getRequestHeaders();
		List<String> ret = headers != null ? headers.get( name) : null;
		return ret != null ? ret : new ArrayList<String>();
	}
	
	/** called if there is no matching URI */
	public void respondNotFound() {
		respondFull( Util.toJson( code, RefCode.NO_SUCH_REQUEST), 400, null);
	}
	
	/** Returns header value or empty string */
	public String getFirstHeader(String name) {
		List<String> headers = getHeaders(name);
		return headers != null && headers.size() > 0 ? headers.get(0) : "";
	}

	public String getLastToken() {
		return Util.getLastToken(m_exchange.getRequestURI().toString(), "/");
	}

	public HttpExchange exchange() {
		return m_exchange;
	}
	
	/** Set by NGINX; could be blocked country or VPN */
	protected boolean isBlockedIP() {
		return getFirstHeader( "X-Block").equals( "1");
	}
	
	protected boolean isNigeria() throws Exception {
		return getCountryCode().equalsIgnoreCase( "NG");
	}
	
	protected String getCountryCode() {
		return Util.left( getFirstHeader( "X-Country-Code"), 2);
	}

	protected String getUserIpAddress() {
		return Util.left( getFirstHeader( "X-Real-IP"), 15);
	}

	/** StringTokenizer would be better, or String split() */
	public String getPostApiToken() {
		int start = m_uri.indexOf( "/api/") + 5;
		int next1 = m_uri.indexOf( "/", start);
		int next2 = m_uri.indexOf( "?", start);
		
		int end = next1 != -1 ? next1 : 1000;
		
		if (next2 != -1) {
			end = Math.min( end, next2);
		}
		
		return Util.substring( m_uri, start, end);
	}
	
	public String getMethod() {
		return m_exchange.getRequestMethod();
	}
	
	public boolean isPost() {
		return "POST".equals( getMethod() );
	}

}
