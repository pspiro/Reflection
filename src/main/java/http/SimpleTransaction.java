package http;

import static reflection.Main.require;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import json.MyJsonObj;
import json.TypedJson;
import reflection.Main;
import reflection.ParamMap;
import reflection.RefCode;
import reflection.RefException;
import tw.util.S;

public class SimpleTransaction {
	public interface MyHttpHandler {
		void handle( SimpleTransaction trans);
	}
	
	private HttpExchange m_exchange;
	
	public HttpExchange exchange() { return m_exchange; } 
	
	public Headers getHeaders() {
		return m_exchange.getRequestHeaders();
	}
	
	public SimpleTransaction( HttpExchange exchange) {
		m_exchange = exchange;
	}

	public static void listen(String host, int port, MyHttpHandler handler) {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
			server.createContext("/favicon", exch -> {} ); // ignore these requests
			server.createContext("/", exch -> handler.handle( new SimpleTransaction( exch) ) ); 
			server.setExecutor( Executors.newFixedThreadPool(10) );
			server.start();
		}
		catch( BindException e) {
			S.out( "The application is already running");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean isPost() {
		return "POST".equals(m_exchange.getRequestMethod() );
	}

	// now write a getJsonRequest
	public String getRequest() throws Exception {
		String uri = m_exchange.getRequestURI().toString().toLowerCase();
		require( uri.length() < 4000, RefCode.UNKNOWN, "URI is too long");

		S.out( "Received %s request %s %s", m_exchange.getRequestMethod(), uri, m_exchange.getHttpContext().getPath() ); 

		if ("GET".equals(m_exchange.getRequestMethod() ) ) {
			String[] parts = uri.split("\\?");
			return parts.length > 1 ? parts[1] : "";
		}
		
		// POST request
		Headers headers = m_exchange.getRequestHeaders();
		String len = headers.getFirst("content-length");
		if (len != null) {
			byte[] bytes = new byte[Integer.valueOf( len) ];
			m_exchange.getRequestBody().read( bytes);
            return new String( bytes);
		}
		return "";
	}
	
	public ParamMap getMap() throws Exception {
		String uri = m_exchange.getRequestURI().toString().toLowerCase();
		require( uri.length() < 4000, RefCode.UNKNOWN, "URI is too long");

		ParamMap map = new ParamMap();

		if ("GET".equals(m_exchange.getRequestMethod() ) ) {
			S.out( "Received GET request %s %s", uri, m_exchange.getHttpContext().getPath() ); 
			// get right side of ? in URL
			String[] parts = uri.split("\\?");

			// if map present, build map of tag/value, expecting tag=value&tag=value
			if (parts.length == 2) {
				String[] params = parts[1].split( "&");
				//map.parseJson( )
				for (String param : params) {
					String[] pair = param.split( "=");
					require( pair.length == 2, RefCode.INVALID_REQUEST, "Tag/value format is incorrect");
					map.put( pair[0], pair[1]);
				}
			}
		}
		
		// POST request
		else {
			try {
	            Reader reader = new InputStreamReader( m_exchange.getRequestBody() );
	            
				JSONParser parser = new JSONParser();
	            JSONObject jsonObject = (JSONObject)parser.parse(reader);  // if this returns a String, it means the text has been over-stringified (stringify called twice)
	            
	            for (Object key : jsonObject.keySet() ) {
	            	Object value = jsonObject.get(key);
	            	require( key instanceof String, RefCode.INVALID_REQUEST, "Invalid JSON, key is not a string");
	            	
	            	if (value != null) {
	            		map.put( (String)key, value.toString() );
	            	}
	            }

	            S.out( "Received POST request " + map.toString() );
			}
			catch( Exception e) {
				e.printStackTrace(); // should never happen
				throw new RefException( RefCode.INVALID_REQUEST, "Error parsing json - " + e.getMessage() ); // no point returning the message text because  
			}
		}
		
		return map;
	}	
	
	public MyJsonObj getJson() throws Exception {
		Main.require( "POST".equals(m_exchange.getRequestMethod() ), RefCode.UNKNOWN, "GET not supported for this endpoint");
		S.out( "received POST w/ len %s", m_exchange.getRequestHeaders().getFirst("content-length") );

		Reader reader = new InputStreamReader( m_exchange.getRequestBody() );
        return new MyJsonObj( new JSONParser().parse(reader) );  // if this returns a String, it means the text has been over-stringified (stringify called twice)
	}	

	/** Only respond once for each request
	 *  @return true if we responded just now. */
	public void respond( String response) {
		try {
			OutputStream outputStream = m_exchange.getResponseBody();
			m_exchange.getResponseHeaders().add( "Content-Type", "application/json");
			m_exchange.sendResponseHeaders(200, response.length());
			outputStream.write(response.getBytes());
			outputStream.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void respondJson(String tag, String val) {
		TypedJson<String> obj = new TypedJson<String>();
		obj.putt( tag, val);
		respond( obj.toString() );
	}

}