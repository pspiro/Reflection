package http;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map.Entry;

import org.json.simple.JSONAware;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import common.Util.ExRunnable;
import reflection.ParamMap;
import reflection.RefCode;
import reflection.RefException;
import test.MyTimer;
import tw.util.S;
import util.LogType;

/** Base class for all classes which handle http requests */
public abstract class BaseTransaction {
	//static final String message = "message";

	protected HttpExchange m_exchange;
	protected boolean m_responded;  // only respond once per transaction
	protected ParamMap m_map = new ParamMap();  // this is a wrapper around JsonObject that adds functionality
	protected String m_uri;
	protected MyTimer m_timer = new MyTimer();
	protected String m_uid;
	
	public BaseTransaction( HttpExchange exchange) {
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
		m_timer.next( "%s ----- %s -------------------------", m_uid, uri);
		return uri;
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

	/** @param responseCode is 200 or 400 */
	protected synchronized boolean respondFull( JSONAware response, int responseCode, HashMap<String,String> headers) {
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
	
	protected synchronized boolean respondWithPlainText( String data) {
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
	protected final void wrap( ExRunnable runnable) {
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
			postWrap();
		}
		catch( Exception e) {
			e.printStackTrace();
			elog( LogType.EXCEPTION, e);
			respondFull(RefException.eToJson(e, RefCode.UNKNOWN), 400, null);
			postWrap();
		}
	}
	
	/** Overridden in subclass */
	protected void postWrap() {
	}

	/** We need this version because some strings have % characters in them */ 
	protected void out( Object format) {
		S.out( m_uid + " " + format);
	}
	
	protected void out( String format, Object... params) {
		S.out( m_uid + " " + format, params);
	}

	protected void olog(LogType type, Object... ar) {
		jlog( type, Util.toJson(ar) );
	}
	
	protected void elog(LogType type, Exception e) {
		jlog(type, RefException.eToJson(e) );
	}

	protected void elog(LogType type, RefException e) {
		jlog(type, e.toJson() );
	}

	/** Overridden in subclass */
	protected void jlog(LogType type, JsonObject json) {
		S.out( "%s LOG %s %s", m_uid, type, json);
	}
	
	/** Parse a POST message and return JsonObject */
	protected JsonObject parseToObject() throws Exception {
		return JsonObject.parse( m_exchange.getRequestBody() );
	}
	
}
