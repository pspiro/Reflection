package http;

import static reflection.Main.require;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import reflection.ParamMap;
import reflection.RefCode;
import reflection.RefException;
import tw.util.S;

class Transaction {
	static void listen(HttpHandler handler, int port) throws Exception {

		// create HTTP server w/ single thread executor
		HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
		server.createContext("/", handler); 
		server.setExecutor( Executors.newFixedThreadPool(5) );  // five threads but we are synchronized for single execution
		server.start();
	}

	public static ParamMap getJson(HttpExchange exch) {
		// TODO Auto-generated method stub
		return null;
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
	static void respond( HttpExchange exchange, String response) {
		try {
			OutputStream outputStream = exchange.getResponseBody();
			exchange.getResponseHeaders().add( "Content-Type", "application/json");
			exchange.sendResponseHeaders(200, response.length());
			outputStream.write(response.getBytes());
			outputStream.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}