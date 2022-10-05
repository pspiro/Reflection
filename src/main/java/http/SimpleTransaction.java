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
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import reflection.ParamMap;
import reflection.RefCode;
import reflection.RefException;
import tw.util.S;

class SimpleTransaction {
	interface MyHttpHandler {
		void handle( SimpleTransaction trans);
	}
	
	private HttpExchange m_exchange;
	
	public SimpleTransaction( HttpExchange exchange) {
		m_exchange = exchange;
		;
		int a = 3;
	}

	public static void listen(String host, int port, MyHttpHandler handler) {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
			server.createContext("/favicon", exch -> {} ); // ignore these requests
			server.createContext("/", exch -> handler.handle( new SimpleTransaction( exch) ) ); 
			server.setExecutor( Executors.newFixedThreadPool(5) );
			server.start();
		}
		catch( BindException e) {
			S.out( "The application is already running");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	// now write a getJsonRequest
	String getRequest() throws Exception {
		String uri = m_exchange.getRequestURI().toString().toLowerCase();
		require( uri.length() < 4000, RefCode.UNKNOWN, "URI is too long");

		if ("GET".equals(m_exchange.getRequestMethod() ) ) {
			S.out( "Received GET request %s %s", uri, m_exchange.getHttpContext().getPath() ); 
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
	
	static ParamMap getMap(HttpExchange exchange) throws Exception {
		String uri = exchange.getRequestURI().toString().toLowerCase();
		require( uri.length() < 4000, RefCode.UNKNOWN, "URI is too long");

		ParamMap map = new ParamMap();

		if ("GET".equals(exchange.getRequestMethod() ) ) {
			S.out( "Received GET request %s %s", uri, exchange.getHttpContext().getPath() ); 
			// get right side of ? in URL
			String[] parts = uri.split("\\?");
			require( parts.length ==2, RefCode.INVALID_REQUEST, "No request present");

			// build map of tag/value, expecting tag=value&tag=value
			String[] params = parts[1].split( "&");
			//map.parseJson( )
			for (String param : params) {
				String[] pair = param.split( "=");
				require( pair.length == 2, RefCode.INVALID_REQUEST, "Tag/value format is incorrect");
				map.put( pair[0], pair[1]);
			}
		}
		
		// POST request
		else {
			try {
	            Reader reader = new InputStreamReader( exchange.getRequestBody() );
	            
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

	/** Only respond once for each request
	 *  @return true if we responded just now. */
	void respond( String response) {
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

}